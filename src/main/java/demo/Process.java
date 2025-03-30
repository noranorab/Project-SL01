package demo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.*;

public class Process extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final int id;
    private List<ActorRef> allProcesses;
    
    private int ballot;
    private int proposal;
    private int readBallot;
    private int imposeBallot;
    private int estimate;
    private HashMap<Integer, State> states;
    private int ackNumber;
    private boolean faultProne;
    private boolean isCrashed;
    private double alpha;
    private boolean decided;
    private int value;
    private boolean onHold;
    private Set<Integer> setOfBallots;
    private HashMap<Integer,Integer> ballotsAck;
 
    
    public Process(int id, double alpha, int n) {
        this.id = id;
        ballot = id - n;
        proposal = -1;
        readBallot = 0;
        imposeBallot = id - n;
        estimate = -1;
        states = new HashMap<Integer, State>();
        ackNumber = 0;
        faultProne = false;
        isCrashed = false;
        this.alpha = alpha;
        decided = false;
        onHold = false;
        setOfBallots = new HashSet<>();
    }

    public static Props props(int id, double alpha, int n) {
        return Props.create(Process.class, () -> new Process(id, alpha, n));
    }
    
    public void propose(int v) {
        int n = allProcesses.size();
        proposal = v;
        ballot = ballot + n;
        states = new HashMap<Integer, State>();
        ackNumber = 0;
        allProcesses.forEach(p -> p.tell(new Read(this.ballot), getSelf()));
        ballotsAck=new HashMap<>();
      
    }

    
    private void logInfo(String message) {
        
        log.info(message);

        
        try {
        	String filename = "process"+".log";
          
            java.io.FileWriter fw = new java.io.FileWriter(filename, true); // 
            fw.write("[" + System.nanoTime() + "] " +"Process"+id+" " + message + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    private boolean checkCrash() {
        if(isCrashed) {
            return true;
        }
        if(decided)return true;
        if(faultProne && Math.random() < alpha) {
            isCrashed = true;
            logInfo("Process " + id + " crashed due to fault-prone behavior.");
            return true;
        }
        return false;
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Crash.class, msg -> {
                if(checkCrash()) return;
                this.faultProne = true;
                logInfo("Process " + id + " received Crash message.");
            })
            
            .match(Launch.class, msg -> {
                if(checkCrash()) return;
                value = msg.value;
                if(!onHold && !decided) {
                    logInfo("Process " + id + " relaunching");
                    propose(msg.value);
                }
            })
            
            .match(Hold.class, ms -> {
                if(checkCrash()) return;
                logInfo("Process " + id + " received Hold message. Entering hold state.");
                onHold = true;
            })
            
            .match(Read.class, msg -> {
                if(checkCrash()) return;
                int ballotReceived = msg.getBallot();
               // log.info(getSender().path().name() + "sent me a read message ");
                if(this.readBallot > ballotReceived || this.imposeBallot > ballotReceived) {
                	
                    getSender().tell(new Abort(ballotReceived), getSelf());
                } else {
                    this.readBallot = ballotReceived;
                    getSender().tell(new Gather(this.id, ballotReceived, this.imposeBallot, this.estimate), getSelf());
                   
                }
            })
            
            .match(Abort.class, msg -> {
                if(checkCrash()) return;
               // logInfo("Process " + id + " received Abort for ballot " + msg.getBallot() + " from " + getSender().path().name());
                if(!onHold && !setOfBallots.contains(msg.getBallot())) {
                   setOfBallots.add(msg.getBallot());
                    getSelf().tell(new Launch(value), getSelf());
                }
            })
            
            .match(Gather.class, msg -> {
                if(checkCrash()) return;
                this.states.put(msg.getSenderId(), new State(msg.getEstimate(), msg.getEstimateBallot()));
                if(states.size() > allProcesses.size()/2) {
                    int maxBallot = Integer.MIN_VALUE;
                    int est = -1;
                    for(State state : states.values()) {
                        if (state.getEstimateBallot() > 0 && state.getEstimateBallot() > maxBallot) {
                            maxBallot = state.getEstimateBallot();
                            est = state.getEstimate();
                        }
                    }
                    if(maxBallot > 0) {
                        this.proposal = est;
                    }
                    this.states = new HashMap<>();
                    allProcesses.forEach(p -> p.tell(new Impose(this.ballot, this.proposal), getSelf()));
                }
            })
            
            .match(Impose.class, msg -> {
                if(checkCrash()) return;
                if(this.readBallot > msg.getBallot() || this.imposeBallot > msg.getBallot()) {
                    getSender().tell(new Abort(msg.getBallot()), getSelf());
                } else {
                    this.estimate = msg.getV();
                    this.imposeBallot = msg.getBallot();
                    getSender().tell(new ACK(msg.getBallot()), getSelf());
                }
            })
            
            .match(ACK.class, msg -> {
                if(checkCrash()) return;
                ++ackNumber;
                if(ackNumber > allProcesses.size()/2) {
                    ackNumber = 0;
                    logInfo("Process "+id +" descided on " +this.proposal);
                    allProcesses.forEach(p -> p.tell(new Decide(this.proposal), getSelf()));
                    decided=true;
                }
            })
            
            .match(Decide.class, msg -> {
                if(checkCrash()) return;
                if(!decided) {
                   logInfo("Process " + id + " received Decide with value " + msg.getV() + " from " + getSender().toString());
                    decided = true;
                   // logInfo("Process " + id + " Decided on value " + msg.getV());
                    allProcesses.forEach(p -> p.tell(new Decide(msg.getV()), getSelf()));
                }
            })
            
            .match(List.class, msg -> {
                if(checkCrash()) return;
                allProcesses = msg;
            })
            .build();
    }
}
