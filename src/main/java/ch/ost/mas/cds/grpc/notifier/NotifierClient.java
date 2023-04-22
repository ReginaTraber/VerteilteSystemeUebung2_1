package ch.ost.mas.cds.grpc.notifier;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * Notifier client
 */
public class NotifierClient {
  
    private static final Logger logger = Logger.getLogger(NotifierClient.class.getSimpleName());

  private final NotifierGrpc.NotifierBlockingStub blockingStub;

  /**
   * Initialize NotifierClient
   * @param channel Channel channel to use for communication
   */
  public NotifierClient(Channel channel) {
      blockingStub = NotifierGrpc.newBlockingStub(channel);
  }

  // Send Note to service
  public void sendNote(String pName, String pMessage) {
    logger.info("Sending note " + pMessage + " by " + pName);
    NoteRequest request = NoteRequest.newBuilder().setName(pName).setMessage(pMessage).build();
    NoteReply   response = null; 
    try {
      response = blockingStub.sendNote(request);
    } catch (StatusRuntimeException e) {
      logger.warning("RPC failed: " +  e.getStatus());
      return;
    }
    logger.info("sendNote: " + response.getMessage());
  }


  public static void main(String[] args) throws Exception {
    String user =  System.getProperty("user.name");
    // Access a service running on the local machine on port 50051
    String target = "localhost:50052";
    // first argument (optional) user name of client
    if (args.length > 0) {
        user = args[0];
    }
    // second argument (optional) service url (hostname:port) 
    if (args.length > 1) {
        target = args[1];
    }

    // Establish communication channel 
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    try {
      NotifierClient client = new NotifierClient(channel);
      for (int i = 0; i < 10; i++) {
          String msg = String.format("Note[%d]", i);
          client.sendNote(user,  msg);
      }
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
