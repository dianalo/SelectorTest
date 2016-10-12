import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class Main {

	public static void main(String[] args) throws IOException{
		
		ServerSocketChannel server = ServerSocketChannel.open().bind(new InetSocketAddress("localhost", 5634));
		server.configureBlocking(false);
		
		Selector sel = Selector.open();
		
		SelectionKey sk2 = server.register(sel, SelectionKey.OP_ACCEPT);
		
		while(true){
			int readyChannels = sel.select();
			
			Set<SelectionKey> skeys = sel.selectedKeys();
			Iterator<SelectionKey> it = skeys.iterator();
			
			while(it.hasNext()){
				SelectionKey s = it.next();
				if(s.isAcceptable()){
					SocketChannel conn = server.accept();
					if(conn != null)
					{
						conn.configureBlocking(false);
						conn.register(sel, SelectionKey.OP_READ);
					}
				}
				else if(s.isReadable()){
					SocketChannel ch = (SocketChannel) s.channel();
					byte[] msg = readFromSocket(ch);
					System.out.println("Received message: " + new String(msg));
					ch.register(sel, SelectionKey.OP_WRITE);
				}
				else if(s.isWritable()){
					SocketChannel ch = (SocketChannel) s.channel();
					ByteBuffer buf = ByteBuffer.wrap("STORED\r\n".getBytes());
					ch.write(buf);
					ch.close();
					System.out.println("Sent message back.");
				}
			}
		}
	}
	
	static byte[] readFromSocket(SocketChannel conn){
		
		byte[] totalReceivedMsg = null;
		
		try{
			ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
			buf.clear();
			int bytesRead;
			while ((bytesRead = conn.read(buf)) != -1) // maybe cover case for 0 bytes are read!!
			{
				buf.flip();
				byte[] roundReceivedMsg = new byte[bytesRead];
				//write from buffer to array
				buf.get(roundReceivedMsg, 0, bytesRead);
				if(totalReceivedMsg == null){
					totalReceivedMsg = new byte[bytesRead]; 
					System.arraycopy(roundReceivedMsg, 0, totalReceivedMsg, 0, bytesRead);
				}
				else{
					totalReceivedMsg = concatenateBytes(totalReceivedMsg, roundReceivedMsg);
				}
				
				buf = ByteBuffer.allocate(BUFFER_SIZE);
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return totalReceivedMsg;
	}
	
	static byte[] concatenateBytes(byte[] a, byte[] b){
		int aLen = a.length;
		int bLen = b.length;
		
		byte[] c = new byte[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		
		return c;
	}
	
	static final int BUFFER_SIZE = 1024;
}
