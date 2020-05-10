
#include <stdio.h>
#include <strings.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/utsname.h>

#define PORT 4444

//variables needed for our beacon initlization
int cmdPort;
int id = 0;
char str1[50];
//////

//socket variables
int sockfd, n;
struct sockaddr_in servaddr, sockaddr;

pthread_t thread_id, thread_id1; //thread varibles

//function to get local OS
void GetLocalOS(char OS[16])
{
    struct utsname operatingsystem;
    uname(&operatingsystem);

    strcpy(OS, operatingsystem.sysname);
    strcat(OS, "-");
    strcat(OS, operatingsystem.release);
 //   printf("Your computer's OS is %s\n", OS);
}

//fucntion to get local time
void GetLocalTime(char thetime[16])
{

    time_t timedetails;
    struct tm *timeinfo;

    time(&timedetails);
    timeinfo = localtime(&timedetails);
    strcpy(thetime, asctime(timeinfo));
   // printf("Current local time and date: %s", asctime(timeinfo));
}

//intilize beacon and set it char message to send
void beacon(int did_user_chooseid)
{
    srand(time(NULL)); //take seed as time

    if (!did_user_chooseid)
    { //only if user did not choose id , it randomly generated

        id = rand() % (10000 + 1 - 1000) + 1000;
    }

    int startUptime = (int)time(NULL);
    int timeInterval = 60;
    char ip[10] = "127.0.0.1";
    cmdPort = rand() % (6000 + 1 - 4500) + 4500; //range from 4500-6000 port number
   
    //conversion to  message

    char str2[50];
    char str3[50];

    //parsing all the required information for a beacon into one string with the charecter c as a delimtter
    sprintf(str1, "%d", id);
    strcpy(str2, "c");
    strcat(str1, str2);
    sprintf(str3, "%d", startUptime);
    strcat(str1, str3);

    strcat(str1, str2);
    sprintf(str3, "%d", timeInterval);
    strcat(str1, str3);

    strcat(str1, str2);

    strcat(str1, ip);

    strcat(str1, str2);
    sprintf(str3, "%d", cmdPort);
    strcat(str1, str3);
}

////helper functions to handle bytes and buffer in TCP transmittion

void convertUpperCase(char *buffer, int length)
{
    int i = 0;
    while (i < length)
    {
        if (buffer[i] >= 'a' && buffer[i] <= 'z')
        {
            buffer[i] = buffer[i] - 'a' + 'A';
        }
        i++;
    }
}

int receive_one_byte(int client_socket, char *cur_char)
{
    ssize_t bytes_received = 0;
    while (bytes_received != 1)
    {
        bytes_received = recv(client_socket, cur_char, 1, 0);
    }

    return 1;
}

int receiveFully(int client_socket, char *buffer, int length)
{
    char *cur_char = buffer;
    ssize_t bytes_received = 0;
    while (bytes_received != length)
    {
        receive_one_byte(client_socket, cur_char);
        cur_char++;
        bytes_received++;
    }

    return 1;
}

void printBinaryArray(char *buffer, int length)
{
    int i = 0;
    while (i < length)
    {
        printf("%d ", buffer[i]);
        i++;
    }

    printf("\n");
}

int toInteger32(char *bytes)
{
    int tmp = (bytes[0] << 24) +
              (bytes[1] << 16) +
              (bytes[2] << 8) +
              bytes[3];

    return tmp;
}

///////////////////////////////////////////////////////////////////

//function used for thread to keep sending beacons every 60 secs or 1 min
void beaconSender(void *vargp)
{

    // fork();
    while (1)
    {
        //printf("the id is: %d\n", id);

        sendto(sockfd, (const void *)&str1, 1000, 0, (struct sockaddr *)NULL, sizeof(servaddr));

        sleep(60);
    }

    return NULL;
}

//function for thread to handle TCP incoming connection from manger
void CmdAgent(void *vargp)
{

    int server_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    printf("server_socket = %d\n", server_socket);

    // bind to a port
    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    // sin.sin_len = sizeof(sin);  // comment this line out if running on pyrite (linux)
    sin.sin_family = AF_INET; // or AF_INET6 (address family)
    sin.sin_port = htons(cmdPort);
    sin.sin_addr.s_addr = INADDR_ANY;

    if (bind(server_socket, (struct sockaddr *)&sin, sizeof(sin)) < 0)
    {
        // Handle the error.
        printf("bind error\n");
    }

    listen(server_socket, 5); /* maximum 5 connections will be queued */
    int counter = 0;
    while (1)
    {
        struct sockaddr client_addr;
        unsigned int client_len;

        printf("accepting ....\n");
        int client_socket = accept(server_socket, &client_addr, &client_len);
        printf("request %d comes ...\n", counter++);

        // processing this request

        // get the command length
        char packet_length_bytes[4];
        receiveFully(client_socket, packet_length_bytes, 4);

        printBinaryArray(packet_length_bytes, 4);

        int packet_length = toInteger32(packet_length_bytes);
        printf("packet_length_bytes = %d\n", packet_length);

        // allocate buffer to receive the data
        char *buffer = (char *)malloc(packet_length);
        receiveFully(client_socket, buffer, packet_length);
        printf("buffer= %c\n", buffer[0]);

        //if I recved command for operating system
        if (buffer[0] == 111)
        {
            char OS[16];
            GetLocalOS(OS);
            // convert to upper case
            convertUpperCase(OS, packet_length);

            // send back
            send(client_socket, packet_length_bytes, 4, 0); // 4 bytes first
            send(client_socket, OS, packet_length, 0);
        }

        if (buffer[0] == 116)
        {
            char thetime[16];
            GetLocalTime(thetime);
            // convert to upper case
            convertUpperCase(thetime, packet_length);

            // send back
            send(client_socket, packet_length_bytes, 4, 0); // 4 bytes first
            send(client_socket, thetime, packet_length, 0);
        }

        free(buffer);
    }

    return NULL;
}

int main(int argc, char *argv[])
{

    char *buffer[100]; //buffer for UDP

    //when making beacon, did user give custom id ?
    if (argc > 1)
    {
        id = atoi(argv[1]); //to force an id to it
        beacon(id);
    }
    else
    {
        beacon(0);
    }

    //setting UDP connection settings
    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    servaddr.sin_port = htons(PORT);
    servaddr.sin_family = AF_INET;

    // create datagram socket
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);

    // connect to server
    if (connect(sockfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        printf("\n Error : Connect Failed \n");
        exit(0);
    }

    //launch threads
    pthread_create(&thread_id, NULL, &beaconSender, NULL);
    pthread_create(&thread_id1, NULL, &CmdAgent, NULL);

    while (1)
    {
    }

    // close the descriptor
    close(sockfd);
}
