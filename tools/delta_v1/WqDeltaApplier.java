package com.kelimebulmacasi.a1;

import java.io.*;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;

/** Streaming, renderer-independent APK delta core. No install or network permissions.
 * All expectations must come from the trusted update manifest. Output is a new file.
 */
public final class WqDeltaApplier {
    private static final long LIMIT=1024L*1024*1024;
    private static final int BUFFER=65536;
    private WqDeltaApplier(){}
    private static void check(boolean ok,String why)throws IOException {
        if(!ok)throw new IOException(why);
    }
    private static MessageDigest digest() throws IOException {
        try{return MessageDigest.getInstance("SHA-256");}
        catch(Exception e){throw new IOException(e);}
    }
    private static String hex(byte[] b){
        StringBuilder s=new StringBuilder(64);
        for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));
        return s.toString();
    }
    private static void checkHash(String expected,String actual)throws IOException {
        check(expected!=null&&expected.matches("[a-fA-F0-9]{64}")&&expected.equalsIgnoreCase(actual),"SHA-256 mismatch");
    }
    private static void checkInterrupted()throws IOException{
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");
    }
    public static String sha(File f)throws IOException{
        MessageDigest md=digest();
        byte[] buf=new byte[BUFFER];
        try(InputStream in=new BufferedInputStream(new FileInputStream(f))){
            int n;while((n=in.read(buf))!=-1){checkInterrupted();md.update(buf,0,n);}
        }
        return hex(md.digest());
    }
    public static void apply(File base,File delta,File output,long baseSize,long deltaSize,long targetSize,
                             String baseHash,String deltaHash,String targetHash)throws IOException{
        check(baseSize>0&&baseSize<=LIMIT&&targetSize>0&&targetSize<=LIMIT&&deltaSize>0&&deltaSize<targetSize,"Invalid sizes");
        check(base.isFile()&&base.length()==baseSize&&delta.isFile()&&delta.length()==deltaSize,"Incorrect file size");
        check(!output.exists(),"Refusing to overwrite output");
        checkHash(baseHash,sha(base));checkHash(deltaHash,sha(delta));
        File dir=output.getAbsoluteFile().getParentFile();
        check(dir!=null&&dir.isDirectory()&&dir.getUsableSpace()>=targetSize+BUFFER,"Insufficient disk");
        File temp=File.createTempFile("wq-delta-",".apk.part",dir);
        boolean committed=false;
        try{
            MessageDigest md=digest();byte[] buf=new byte[BUFFER];
            long written=0;int ops=0;
            try(RandomAccessFile source=new RandomAccessFile(base,"r");
                DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(delta))));
                FileOutputStream out=new FileOutputStream(temp)){
                check(in.readInt()==0x57514431&&in.readLong()==targetSize,"Invalid delta header");
                while(true){
                    checkInterrupted();int type=in.readUnsignedByte();
                    if(type==0)break;
                    check(++ops<=1000000,"Too many operations");
                    long offset=type==1?in.readLong():0;
                    check(type==1||type==2,"Unknown operation");
                    long count=Integer.toUnsignedLong(in.readInt());
                    check(count>0&&count<=targetSize-written,"Output size exceeds target");
                    if(type==1){
                        check(offset>=0&&offset<=baseSize&&count<=baseSize-offset,"Invalid copy range");
                        source.seek(offset);
                    }
                    long left=count;
                    while(left>0){
                        checkInterrupted();
                        int n=(int)Math.min(left,buf.length);
                        if(type==1)source.readFully(buf,0,n);else in.readFully(buf,0,n);
                        out.write(buf,0,n);md.update(buf,0,n);left-=n;
                    }
                    written+=count;
                }
                check(written==targetSize&&in.read()==-1,"Incomplete output or trailing data");
                checkHash(targetHash,hex(md.digest()));
                out.getFD().sync();
            }
            check(!output.exists()&&temp.renameTo(output),"Unable to commit verified APK");
            committed=true;
        }finally{if(!committed)temp.delete();}
    }
    /** CI only: same class can be compiled on JVM without an Android device. */
    public static void main(String[] args)throws IOException{
        if(args.length!=9)throw new IOException("base delta output baseSize deltaSize targetSize baseHash deltaHash targetHash");
        apply(new File(args[0]),new File(args[1]),new File(args[2]),Long.parseLong(args[3]),
            Long.parseLong(args[4]),Long.parseLong(args[5]),args[6],args[7],args[8]);
    }
}
