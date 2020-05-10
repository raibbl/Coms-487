
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

//variables needed for our buffer and connections

int port = 0;
char str1[50];
char bufferlength[4];

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

    sprintf(OS, " %s-%s TRUE ", operatingsystem.sysname, operatingsystem.release);
}

//fucntion to get local time
void GetLocalTime(char thetime[16])
{

    time_t timedetails;
    struct tm *timeinfo;

    time(&timedetails);
    timeinfo = localtime(&timedetails);
    sprintf(thetime, " 0%d0%d0%d TRUE ", timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);
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
        printf("%x ", buffer[i]);
        i++;
    }

    printf("\n");
}

int toInteger32(char *bytes)
{

    // Otherwise perform the 2's complement math on the value

    int tmp = (bytes[0] >> 8) +
              (bytes[1] >> 16) +
              (bytes[2] >> 24) +

              bytes[3];

    return tmp;
}

///////////////////////////////////////////////////////////////////

//function for thread to handle TCP incoming connection from RPC client
void CmdProcessor(void *vargp)
{
    //start a TCP socket
    int server_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    printf("server_socket = %d\n", server_socket);

    // bind to a port
    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    // sin.sin_len = sizeof(sin);  // comment this line out if running on pyrite (linux)
    sin.sin_family = AF_INET; // or AF_INET6 (address family)

    sin.sin_port = htons(port);
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
        //accept incoming requests
        int client_socket = accept(server_socket, &client_addr, &client_len);
        printf("request %d comes ...\n", counter++);

        // processing this request
        //intilize first incoming buffer that would have CMD ID and length
        // get the size of the incoming buffer.
        char packet_length_bytes[104];
        receiveFully(client_socket, packet_length_bytes, 104);
        int m = 0;

        //translating my length from the bytes to our string written hex and finall to our charcter numbers to know what is the length
        //from hex to ascII of numbers of packet length

        int j = 0;
        int anumber = 100;

        for (j = 0; j < 4; j++)
        {
            char achar = (char)packet_length_bytes[anumber++];
            bufferlength[j] = achar;
        }

        //our data part of the buffer size

        int number = (int)strtol(bufferlength, NULL, 16);
        // printf("%d ", number);
        ///////////////////////////
        //print the size
        printBinaryArray(packet_length_bytes, 104);

        printf("packet_length_bytes = %d\n", number + 104);

        //make a buffer that can handle our CMDID + length + buffer data
        // allocate buffer to receive the data
        char *buffer = (char *)malloc(number + 104);
        receiveFully(client_socket, buffer, number + 104);

        //if I recved command for operating system
        if (buffer[8] == 79)
        {
            char OS[16];
            GetLocalOS(OS);

            // convert to upper case

            //add our OS details to our buffer
            strcat(buffer, OS);

            convertUpperCase(buffer, number + 104);

            // send back

            send(client_socket, buffer, number + 104, 0);
        }
        //if I recieve command for time
        else if (buffer[8] == 84)
        {
            char thetime[16];
            GetLocalTime(thetime);

            //add the time results to our buffer
            strcat(buffer, thetime);

            convertUpperCase(buffer, number + 104);

            // send back

            send(client_socket, buffer, number + 104, 0);
        }
        else
        {
            break;
        }

        free(buffer);
    }

    return NULL;
}

int main(int argc, char *argv[])
{

    port = 7000;
    //was a custom port givem?
    if (argc > 1)
    {
        port = atoi(argv[1]); //TCP port specfied
    }

    pthread_create(&thread_id1, NULL, &CmdProcessor, NULL);
    pthread_join(thread_id1);

    while (1)
    { //waiting for connection
    }
}
