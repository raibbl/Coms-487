import java.nio.*;
import java.io.*;
import java.util.*;
public class c_char {
    byte[] buf;


    public c_char() {
        //just make the object
    }

public int getsize(){
        return buf.length;
}


    public String toString(){




        StringBuilder data = new StringBuilder();

        for (int i = 0; i < buf.length; i++) {///convert from byte[] to StringBuilder
            if (buf[i] == 0) {
                break; // end of transmittion
            }
            char c = (char) buf[i];


            data.append(c);
        }

        String recv = data.toString();//convert from stringbuilder to string and read the beacon
        return recv;
    } // the   string  value represented by buf

    public void setValue( byte b[]){

        for(int i=0;i<buf.length;i++){
            buf[i]=b[i];
        }
    }// copy the value in b into buf

    public void setValue(String v){
      buf=v.getBytes();
    } // set buf according to v

    public byte[] toByte(){
        return buf;
    } // return buf

//just testing :)

/*
    public static void main(String[] args)
    {

        c_char test = new c_char();
        String c = "hello";
        test.setValue(c);
        System.out.println(test.toString());
        byte[] bytes = c.getBytes();
        for (byte b : bytes) {
        System.out.format("0x%x \n ", b);
    }
        byte[] array = test.toByte();

        System.out.println("this is what I get");

        for (byte b : array) {
        System.out.format("0x%x \n ", b);
    }




*/






}
