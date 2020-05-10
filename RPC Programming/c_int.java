import java.nio.*;
public class c_int {
    byte[] buf =  new byte[4]; // little endian


    public c_int() {
     //just make the object
    }

    public int getSize(){
        return  buf.length;
    } // the size of buf

    public int getValue(){
        return ByteBuffer.wrap(buf).getInt();
    } // the int value represented by buf
    public void setValue(byte[] b){

        for(int i=0;i<buf.length;i++){
            buf[i]=b[i];
        }
    }// copy the value in b into buf

    public void setValue(int v){
        byte[] result = new byte[4];
        result[0] = (byte) (v>> 24);
        result[1] = (byte) (v >> 16);
        result[2] = (byte) (v>> 8);
        result[3] = (byte) (v /*>> 0*/);
        buf = result;
    } // set buf according to v

    public byte[] toByte(){
        return buf;
    } // return buf

//just testing :)
    /*public static void main(String[] args)
    {

        c_int test = new c_int();
        byte[] bytes = ByteBuffer.allocate(4).putInt(5000).array();
        test.setValue(bytes);
        System.out.println("this is what I set");

        for (byte b : bytes) {
        System.out.format("0x%x \n ", b);
    }
        byte[] array = test.toByte();

        System.out.println("this is what I get");

        for (byte b : array) {
        System.out.format("0x%x \n ", b);
    }

        System.out.println("my int value"+test.getValue());

    test.setValue(100);
        System.out.println(test.getValue());



    }*/

}
