import java.io.File;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.AssociationImpl;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import jdk.internal.org.jline.utils.ShutdownHooks.Task;

/**
 * 
 */

/**
 * @author Marin Bressel
 *
 */
public class Test {

	/**
	 * @param args
	 */
	// endergebniss
	Collection<Prozess> prozesse = new ArrayList<Prozess>();
	// alle elemente die schon abgearbeitet wurden
	Collection<FlowNode> elem = new ArrayList<FlowNode>();
	// alle prozesse an denen schon gearbeitet wurde
	Collection<Prozess> workInProgress = new ArrayList<Prozess>();
	Collection<FlowNode> gates = new ArrayList<FlowNode>();
	Stack<Prozess> prozessStack = new Stack<Prozess>();
	Stack<Prozess> schritt2Prozesse = new Stack<Prozess>();
	Stack<Integer> schritt2Zähler = new Stack<Integer>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub		
		
		File file = new File("M:\\Parktikum Git\\Hope\\src\\main\\resources\\UC4_Amazon.bpmn");
		File file1 = new File ("M:\\Parktikum Git\\Hope\\src\\main\\resources\\forTypes.bpmn");
		Test test = new Test();
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		BpmnModelInstance modelInstance1 = Bpmn.readModelFromFile(file1);
		Collection<ModelElementInstance> assos = modelInstance.getModelElementsByType(modelInstance1.getModelElementById("Association_0iy4pom").getElementType());
		FlowNode Startevent = modelInstance.getModelElementById("StartEvent_1");
		Collection<Prozess> res = (test.finalMapping(modelInstance, Startevent));
		for (Prozess out : res) {
			System.out.println(out.getName() + ":=" + out.getContent());
		}
	}

	// Methode um rauszufinden, ob eine Collection einen Prozess mit gewissem Namen
	// hat
	public boolean containsProc(Collection<Prozess> prozesse, String name) {
		for (Prozess p : prozesse) {
			if (p.getName().contains(name)) {
				return true;
			}
		}
		return false;
	}

	public Collection<Prozess> finalMapping(BpmnModelInstance modelInstance, FlowNode Startevent) {
		mapping(modelInstance, Startevent, 1, 0);
		while (!schritt2Prozesse.isEmpty()) {
			Prozess p = schritt2Prozesse.pop();
			p.setContent(p.getContent() + "*P" + schritt2Zähler.pop());
		}
		return prozesse;
	}

	// Mapt die Modelinstance von dem Startevent aus zu ACP. count ist ein Zähler
	// welcher Prozess der letzte war und currproc ist der Aktuelle prozess an dem
	// gearbeitet wird
	public Collection<Prozess> mapping(BpmnModelInstance modelInstance, FlowNode Startevent, int count, int currproc) {
		// Das Aktuelle Element mit dem gearbeitet wird.
		FlowNode curr = Startevent;
		// Startevent wurde schon abgearbeitet
		elem.add(Startevent);
		// Neuen Prozess erstellen
		Prozess p = new Prozess();
		// Kopie vom count. Hilft bei verhinderung von Doppelungen
		int countcopy = count;
		p.setName("P" + currproc);
		p.setFirst(Startevent);
		p.acts.add(Startevent);
		// p ist workInProgress
		// currproc=currproc+1;
		// Bis zu einem Gateway können alle Sachen hinzugefügt werden als Aktivitäten
		p.setContent(curr.getName());
		workInProgress.add(p);
		// Gucken ob das Aktuelle element ein Endevent ist, wenn ja. zur endmenge
		// hinzufügen und beenden.
		if (curr.getClass().toString().contains("EndEvent")) {
			prozesse.add(p);
			workInProgress.add(p);
			return prozesse;
		}

		// So lange wie kein Gateway dran kommt, wird der Inhalt vom aktuellen Prozess
		// passend erweitert.
		while (!curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {

			// wenn das nächste element ein Endevent ist, dann wird das auch einfach
			// hinuzgefügt und es wird beendet.(steht im else block)
			if (!curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("EndEvent")) {
				curr = (FlowNode) curr.getOutgoing().iterator().next().getTarget();
				p.setContent(p.getContent() + "*" + curr.getName());
				p.acts.add(curr);
			} else {
				p.setContent(p.getContent() + "*" + curr.getOutgoing().iterator().next().getTarget().getName());
				p.acts.add(curr.getOutgoing().iterator().next().getTarget());
				prozesse.add(p);
				return prozesse;
			}

		}

		// Sobald das nöchste Element ein Gateway ist, werden neue Prozesse für jeden
		// ausgehenden Strank erstellt.
		// Für Exclusive Gateways (gucken ob das nächste ein XOR gateway ist)
		if (curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ExclusiveGateway")) {
			// curr wird zum Gateway gemacht
			curr = (FlowNode) curr.getOutgoing().iterator().next().getTarget();
			// prüfen ob es ein aufspaltendes Gateway ist oder ein zusammenführendes anhand
			// der ausgehenden Äste (size>1 heißt aufspaltend)
			if (curr.getOutgoing().size() > 1) {
				// content um *( erweitern
				p.setContent(p.getContent() + "*(");
				// für jeden ausgehenden Ast, wird etwas hinzugefügt. Alles jeweil von einem +
				// getrennt
				for (SequenceFlow proz : curr.getOutgoing()) {
					// bei endevents wird nur der Name hinzugefüht. und beendet.
					if (proz.getTarget().getClass().toString().contains("EndEvent")) {
						p.setContent(p.getContent() + proz.getTarget().getName() + "+");
						p.acts.add(proz.getTarget());
					} else {

						if (!elem.contains((FlowNode) proz.getTarget().getOutgoing().iterator().next().getTarget())) {
							while (containsProc(workInProgress, "P" + countcopy)) {
								countcopy++;
							}
							p.setContent(p.getContent() + "P" + countcopy + "+");
							// subprozesse für die jeweiligen Stränge erstellen
							// Wenn 2 oder mehr gateways folgen
							while (proz.getTarget().getClass().toString().contains("Gateway")) {
								if (proz.getTarget().getOutgoing().size() > 1) {
									// TODO
								} else {
									proz = proz.getTarget().getOutgoing().iterator().next();
								}
							}

							mapping(modelInstance, proz.getTarget(), count + curr.getOutgoing().size(), countcopy);
							count++;
							countcopy++;

						} else {
							for (Prozess x : workInProgress) {
								if (x.getFirst() == proz.getTarget().getOutgoing().iterator().next().getTarget()) {
									p.setContent(p.getContent() + x.getName() + "+");
									break;
								}
							}
						}
					}
				}
				p.setContent(StringUtils.chop(p.getContent()) + ")");

			} else {
				while (curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
					if (curr.getOutgoing().iterator().next().getTarget().getOutgoing().size() > 1) {
						// TODO
					} else {
						curr = curr.getOutgoing().iterator().next().getTarget();
					}
				}
				if (!elem.contains(curr.getOutgoing().iterator().next().getTarget())) {
					while (containsProc(workInProgress, "P" + countcopy)) {
						countcopy++;
					}
					p.setContent(p.getContent() + "*P" + countcopy);
					mapping(modelInstance, curr.getOutgoing().iterator().next().getTarget(), count + 1, countcopy);
					count++;
					countcopy++;
				} else {
					for (Prozess x : workInProgress) {
						if (x.getFirst() == curr.getOutgoing().iterator().next().getTarget()) {
							p.setContent(p.getContent() + "*" + x.getName());
							break;
						}
					}
				}
			}
		}

		// Für Parallele Gateways
		if (curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ParallelGateway")) {

			curr = (FlowNode) curr.getOutgoing().iterator().next().getTarget();
			if (curr.getOutgoing().size() > 1) {
				prozessStack.push(p);
				p.setContent(p.getContent() + "*(");
				for (SequenceFlow proz : curr.getOutgoing()) {
					if (!elem.contains((FlowNode) proz.getTarget().getOutgoing().iterator().next().getTarget())) {
						while (containsProc(workInProgress, "P" + countcopy)) {
							countcopy++;
						}
						p.setContent(p.getContent() + "P" + countcopy + "||");

						while (proz.getTarget().getClass().toString().contains("Gateway")) {

							if (proz.getTarget().getOutgoing().size() > 1) {
								// TODO
							} else {
								proz = proz.getTarget().getOutgoing().iterator().next();

							}
						}
						mapping(modelInstance, proz.getTarget(), count + curr.getOutgoing().size(), countcopy);
						count++;
						countcopy++;
					} else {

						for (Prozess x : workInProgress) {
							if (x.getFirst() == proz.getTarget().getOutgoing().iterator().next().getTarget()) {
								p.setContent(p.getContent() + x.getName() + "||");
								break;
							}
						}
					}
				}
				p.setContent(StringUtils.chop(StringUtils.chop(p.getContent())) + ")");

			} else {
				while (containsProc(workInProgress, "P" + countcopy)) {
					countcopy++;
				}

				FlowNode curr2 = curr;
				while (curr2.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
					if (curr2.getOutgoing().iterator().next().getTarget().getOutgoing().size() > 1) {
						// TODO
					} else {
						curr2 = curr2.getOutgoing().iterator().next().getTarget();
					}
				}

				if (!elem.contains(curr2.getOutgoing().iterator().next().getTarget())) {
					if (!gates.contains(curr)) {
						gates.add(curr);
						schritt2Prozesse.push(prozessStack.pop());
						schritt2Zähler.push(countcopy);
						while (curr.getOutgoing().iterator().next().getTarget().getClass().toString()
								.contains("Gateway")) {
							if (curr.getOutgoing().iterator().next().getTarget().getOutgoing().size() > 1) {
								// TODO
							} else {
								curr = curr.getOutgoing().iterator().next().getTarget();
							}
						}

						mapping(modelInstance, curr.getOutgoing().iterator().next().getTarget(), count + 1, countcopy);
						count++;
						countcopy++;
					}
				} else {

				}

//						while(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
//							if(curr.getOutgoing().iterator().next().getTarget().getOutgoing().size()>1) {
//								//TODO
//							}else {
//								curr = curr.getOutgoing().iterator().next().getTarget();
//							}
//						}
//						
//						if(!elem.contains(curr.getOutgoing().iterator().next().getTarget())) {
//							while (containsProc(workInProgress, "P"+countcopy)) {
//								countcopy++;
//							}
//							p.setContent(p.getContent()+"*P"+countcopy);
//							
//								mapping(modelInstance, curr.getOutgoing().iterator().next().getTarget(), count+1, countcopy);
//								count++;
//								countcopy++;						
//						}else {
//							for(Prozess x : workInProgress) {
//								if(x.getFirst()==curr.getOutgoing().iterator().next().getTarget()) {
//									p.setContent(p.getContent()+"*"+x.getName());
//									break;
//								}
//							}
//						}
			}
		}

		prozesse.add(p);
		workInProgress.add(p);
		// else
		// if(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ParallelGateway")){}
		return prozesse;
	}

	// veraltet
//	public FlowNode bisXORAusgeben (BpmnModelInstance modelInstance, FlowNode first, int outgoing, int curr) {
//		while (!first.getClass().toString().contains("ExclusiveGateway")&&!(first.getIncoming().size()>1)) {
//			
//			if(first.getClass().toString().contains("StartEvent")||first.getClass().toString().contains("Task") ||first.getClass().toString().contains("IntermediateThrowEvent") ) {
//				
//				if(first.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ExclusiveGateway")&&first.getOutgoing().iterator().next().getTarget().getIncoming().size()>1) {
//					if(curr<outgoing) {
//						res = res+first.getName()+")+(";
//						System.out.println(first.getName());
//					}else {
//						res = res+first.getName()+")";
//						System.out.println(first.getName());
//					}
//					
//				}else{
//					
//					res = res+first.getName()+"*";
//					System.out.println(first.getName());
//					
//				}
//				
//			}else if(first.getClass().toString().contains("ExclusiveGateway")){
//				
//				if(first.getOutgoing().size()>1) {
//					
//					 int outgoingFlows = first.getOutgoing().size();
//					 int i=1;
//					for (SequenceFlow flow : first.getOutgoing()) {
//						
//						
//						first = bisXORAusgeben(modelInstance, flow.getTarget(),outgoingFlows, i );
//						i++;
//					}
//					
//				}else if(first.getIncoming().size()>1) {
//					
//				}
//			
//			}else if(first.getClass().toString().contains("ParallelGateway")) {
//				
//			}
//			first=first.getOutgoing().iterator().next().getTarget();
//			
//		}
//		
//		return first;
//	}
//	
//	public String alleAusgeben (BpmnModelInstance modelInstance, FlowNode first) {
//		
//		
//		while (!first.getClass().toString().contains("EndEvent")) {
//		
//			if(first.getClass().toString().contains("StartEvent")||first.getClass().toString().contains("Task") ||first.getClass().toString().contains("IntermediateThrowEvent") ) {
//				
//				res = res+first.getName()+"*";
//				System.out.println(first.getName());
//			
//			}else if(first.getClass().toString().contains("ExclusiveGateway")){
//				
//				if(first.getOutgoing().size()>1) {
//					
//					int outgoingFlows = first.getOutgoing().size();
//					int i=1;
//					System.out.println(outgoingFlows);
//					res=res+"("; 
//					for (SequenceFlow flow : first.getOutgoing()) {
//						
//						System.out.println(i);
//						first = bisXORAusgeben(modelInstance, flow.getTarget(),outgoingFlows,i);
//						i++;
//					}
//					
//				}else if(first.getIncoming().size()>1) {
//					
//				}
//			
//			}else if(first.getClass().toString().contains("ParallelGateway")) {
//				
//			}
//			
//		}
//		res = res+first.getName();
//		System.out.println(first.getName());
//		
//		return res;
//	}
//	
//	
//	
//	

}
