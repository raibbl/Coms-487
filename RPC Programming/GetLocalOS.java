import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

class GetLocalOS {

    static c_char OS = new c_char();
    static c_char valid = new c_char();

    static private byte[] toBytes(int i) {
        byte[] result = new byte[4];

        result[0] = (byte) (i >>> 8);
        result[1] = (byte) (i >>> 16);
        result[2] = (byte) (i >>> 24);
        result[3] = (byte) (i /* >> 0 */);

        return result;
    }

    public static int execute(String ip, int port) {
        // intilize the length of the buffer
        int length = OS.getsize() + valid.getsize();

        // translate the length as hex string
        String length_as_string = Integer.toHexString(length);

        // translate hex string to bytes to send in buffer
        byte[] LengthByte = length_as_string.getBytes();

        // finally make the buffer according to dynamic size of time & valid
        byte[] buf = new byte[100 + 4 + length];

        // initlize the header for setting the command ID
        String Header = "GetLocalOS";
        byte[] HeaderByte = Header.getBytes();

        // copy the header to buffer
        for (int i = 0; i < HeaderByte.length; i++) {
            buf[i] = HeaderByte[i];

        }

        // copy length to buffer
        System.arraycopy(LengthByte, 0, buf, 100, LengthByte.length);

        try {
            String serverIP = ip;

            // create a socket
            Socket clientSocket = new Socket(serverIP, port);

            // create text reader and writer
            DataInputStream inStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outStream = new DataOutputStream(clientSocket.getOutputStream());

            // send the bufffer header to let the RPC server know the buffer size
            outStream.write(buf, 0, 104);
            outStream.flush();

            // after telling them about the buffersize now send the actual empty buffer

            outStream.write(buf, 0, buf.length);
            outStream.flush();
            // read the data back

            inStream.readFully(buf);

            // convert the binary bytes to string
            String ret = new String(buf);

            // parse the reaction of the server
            String[] bufferinfo = ret.split(" ");

            // set our values
            OS.setValue(bufferinfo[1]);
            valid.setValue(bufferinfo[2]);

        } catch (Exception e) {
            // TODO: handle exception
        }

        return 0;
    }
    // for testing
    // public static void main(String[] args) {
    // int port = 7000; //

    // Scanner sc = new Scanner(System.in);
    // port = sc.nextInt();

    // valid.setValue("false");
    // OS.setValue("windows12345");
    // System.out.println(valid.getsize());
    // execute(0, port);
    // System.out.println(valid.toString());
    // System.out.println(OS.toString());

    // }

}
