
package ch.ost.mas.cds.grpc.notifier;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.protobuf.Empty;

import ch.ost.mas.cds.grpc.notifier.NotifierGrpc.NotifierImplBase;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;


/**
 * Service that manages startup/shutdown of a {@code Greeter} server.
 */
public class NotifierService {

    private static final Logger logger = Logger.getLogger(NotifierService.class.getSimpleName());

    private Server              server;
    private String              ownerName = "<undefined>";
    private boolean             wasUsed;
    private int                 invoked;
    private Date                invokedResetAt;

    public NotifierService(String pArgs[]) {
        if (pArgs.length > 0) {
            ownerName = pArgs[0];
        }
        invokedResetAt = new Date();
    }
    private void start()  throws IOException {
        /* The port on which the server should run */
        int port = 50052;
        server = ServerBuilder.forPort(port)
                .addService(new NotifierImpl(this))
                .build()
                .start();
        log("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    NotifierService.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop()
            throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown()
            throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    /**
     * hasBeenUsed Reads and resets the used flag
     * 
     * @return true if the service has been used since the last request
     */
    public boolean hasBeenUsed() {
        boolean used = wasUsed;
        wasUsed = false;
        return (used);
    }
    
    private String invokedSince() {
        String invokedRecord = String.format("Invoked %d since %s", invoked, formatDate(invokedResetAt));
        invokedResetAt = new Date();
        invoked = 0;
        return invokedRecord;
    }
    
    private String getName() {
        return ownerName;
    }
    
    private int invoked() {
        wasUsed = true;
        return (++invoked);
    }

    private  void  log(String pLogMsg) {
        String timestamp = formatDate(new Date());
        logger.info(String.format("[%s] %s", timestamp, pLogMsg));
    }
    
    private String formatDate(Date pDate) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(pDate);
    }
    
    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final NotifierService server = new NotifierService(args);
        server.start();
        server.blockUntilShutdown();
    }

    static class NotifierImpl extends NotifierImplBase {
        
        private static NotifierService tie;
        
        NotifierImpl(NotifierService pServ) {
            tie = pServ;
        }
        
        @Override
        public void getName(Empty pRequest, StreamObserver<NameReply> pResponseObserver) {
            NameReply reply = NameReply.newBuilder().setName(tie.getName()).build();
            pResponseObserver.onNext(reply);
            pResponseObserver.onCompleted();
            tie.invoked();
        }

        @Override
        public void sendNote(NoteRequest pRequest, StreamObserver<NoteReply> pResponseObserver) {
            String replyMsg = new StringBuilder()
                    .append("Message: ")
                    .append(pRequest.getMessage())
                    .append(" from ")
                    .append(pRequest.getName())
                    .toString();
            
            NoteReply reply = NoteReply.newBuilder().setMessage(replyMsg).build();
            pResponseObserver.onNext(reply);
            pResponseObserver.onCompleted();
            tie.log(replyMsg);
            tie.invoked();
        }

    }
}
