package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.Collections;

public class Main {
	public static void main(String[] args) throws InterruptedException {

		int N=10;
		int f=4;
		int tle=30;
		double alpha=0;
		ActorSystem system = ActorSystem.create("ConsensusSystem");
		List<ActorRef> processes = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            ActorRef p = system.actorOf(Process.props(i, alpha,N), "process" + i);
            processes.add(p);
        }
       
      processes.forEach(p -> p.tell(processes,ActorRef.noSender() ));
        
        Collections.shuffle(processes);
        for (int i = 0; i < f; i++) {
        	processes.get(i).tell(new Crash(), ActorRef.noSender());
		}
       // Thread.sleep(400);
        for (int i = 0; i < N; i++) {
        	Random random = new Random();
            int value = random.nextInt(2);
			processes.get(i).tell(new Launch(value), ActorRef.noSender());
		}
        
        Thread.sleep(tle);
		Random r = new Random();
		int leader = r.nextInt(N - f) + f;
		
		logInfo("Leader=" + processes.get(leader).toString(),system);
		for (int i = 0; i < N; i++) {
			if (i != leader)
				processes.get(i).tell(new Hold(), ActorRef.noSender());
		}

		try {
			waitBeforeTerminate();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			system.terminate();
		}
	}
	public static void waitBeforeTerminate() throws InterruptedException {
		Thread.sleep(5000);
	}
	
    private static void logInfo(String message,ActorSystem system) {
        // Console logging (optional)
    	system.log().info(message);

        // Manual file logging
        try {
            String filename = "process"+".log";
            
            java.io.FileWriter fw = new java.io.FileWriter(filename, true); // 'true' for appending
            fw.write("[" + System.nanoTime() + "] " +"Main :  " + message + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

