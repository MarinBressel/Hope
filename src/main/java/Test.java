import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.cmmn.instance.Task;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import com.sun.jdi.event.Event;



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
	
	Collection <Prozess> prozesse = new ArrayList<Prozess>();
	Collection <FlowNode> elem = new ArrayList <FlowNode>();
	Collection <Prozess> workInProgress = new ArrayList <Prozess>();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		File file = new File("M:\\Deluoode\\eclipse-workspace\\CamundaTest\\src\\main\\resources\\diagram_6.bpmn");
		Test test = new Test();
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(file);
		FlowNode Startevent = modelInstance.getModelElementById("Event_1m7yum0");
		Collection<Prozess> res = (test.mapping(modelInstance, Startevent, 1, 0));
		for(Prozess out : res) {
			System.out.println(out.getName()+":="+out.getContent());
		}
	}
	
	
	public boolean containsProc (Collection<Prozess> prozesse, String name) {
		
		for(Prozess p: prozesse) {
			if (p.getName().contains(name)){
				return true; 
			}
		}
		return false;
	}
	
	
	
	
	
	
	public Collection <Prozess> mapping (BpmnModelInstance modelInstance, FlowNode Startevent,int count,int currproc ) {

		//Das Aktuelle Element mit dem gearbeitet wird. 
			FlowNode curr = Startevent;
			elem.add(Startevent);
			//Neuen Prozess erstellen
			Prozess p = new Prozess();
			int countcopy = count;
			p.setName("P"+currproc);
			p.setFirst(Startevent);
			workInProgress.add(p);
			
			//currproc=currproc+1;
			//Bis zu einem Gateway können alle Sachen hinzugefügt werden als Aktivitäten
			p.setContent(curr.getName());
			if(curr.getClass().toString().contains("EndEvent")) {
				prozesse.add(p);
				return prozesse;
			}
				while(!curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
					if(!curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("EndEvent")) {
						curr=(FlowNode)curr.getOutgoing().iterator().next().getTarget();
						p.setContent(p.getContent()+"*"+curr.getName());
					
					}else {
						p.setContent(p.getContent()+"*"+curr.getOutgoing().iterator().next().getTarget().getName());
						prozesse.add(p);
						return prozesse;	
					}
			
				}
			
				//Sobald ein Gateway kommt werden neue Prozesse für jeden ausgehenden Strank erstellt.
				//Für Exclusive Gateways
				if(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ExclusiveGateway")) {
					curr=(FlowNode)curr.getOutgoing().iterator().next().getTarget();
					if(curr.getOutgoing().size()>1) {
						p.setContent(p.getContent()+"*(");
						for (SequenceFlow proz : curr.getOutgoing()) {
							if(proz.getTarget().getClass().toString().contains("EndEvent")) {
								p.setContent(p.getContent()+proz.getTarget().getName()+")");
								prozesse.add(p);
								return prozesse;
							}
							if(!elem.contains((FlowNode)proz.getTarget().getOutgoing().iterator().next().getTarget())) {
								while(containsProc(workInProgress, "P"+countcopy)) {
									countcopy++;
								}
								p.setContent(p.getContent()+"P"+countcopy+"+");
								//subprozesse für die jeweiligen Stränge erstellen
								//Wenn 2 oder mehr gateways folgen 
								while(proz.getTarget().getClass().toString().contains("Gateway")) {
									if(proz.getTarget().getOutgoing().size()>1) {
										//TODO
									}else {
										proz = proz.getTarget().getOutgoing().iterator().next();
									}
								}
								
								mapping(modelInstance, proz.getTarget(), count+curr.getOutgoing().size(), countcopy);
								count++;
								countcopy++;
								
							}else {
								for(Prozess x : workInProgress) {
									if(x.getFirst()==proz.getTarget().getOutgoing().iterator().next().getTarget()) {
										p.setContent(p.getContent()+x.getName()+"+");
										break;
									}
								}
							}
						}
						p.setContent(StringUtils.chop(p.getContent())+")");
						
					}else {
						while(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
							if(curr.getOutgoing().iterator().next().getTarget().getOutgoing().size()>1) {
								//TODO
							}else {
								curr = curr.getOutgoing().iterator().next().getTarget();
							}
						}
						if(!elem.contains(curr.getOutgoing().iterator().next().getTarget())) {
							while (containsProc(workInProgress, "P"+countcopy)) {
								
								countcopy++;
							}
							p.setContent(p.getContent()+"*P"+countcopy);
							mapping(modelInstance, curr.getOutgoing().iterator().next().getTarget(), count+1, countcopy);
							count++;
							countcopy++;
						}else {
							for(Prozess x : workInProgress) {
								if(x.getFirst()==curr.getOutgoing().iterator().next().getTarget()) {
									p.setContent(p.getContent()+"*"+x.getName());
									break;
								}
							}
						}
					}	
				}
				
				//Für Parallele Gateways
				if(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ParallelGateway")) {
					curr=(FlowNode)curr.getOutgoing().iterator().next().getTarget();
					if(curr.getOutgoing().size()>1) {
						p.setContent(p.getContent()+"*(");
						for (SequenceFlow proz : curr.getOutgoing()) {
							if(!elem.contains((FlowNode)proz.getTarget().getOutgoing().iterator().next().getTarget())) {
								while (containsProc(workInProgress, "P"+countcopy)) {
									
									countcopy++;
								}
								p.setContent(p.getContent()+"P"+countcopy+"||");
								//subprozesse für die jeweiligen Stränge erstellen
								while(proz.getTarget().getClass().toString().contains("Gateway")) {
									
									if(proz.getTarget().getOutgoing().size()>1) {
										//TODO
									}else {
										proz = proz.getTarget().getOutgoing().iterator().next();
										
									}
								}
								mapping(modelInstance, proz.getTarget(), count+curr.getOutgoing().size(), countcopy);
								count++;
								countcopy++;
								
							}else {
								
								for(Prozess x : workInProgress) {
									if(x.getFirst()==proz.getTarget().getOutgoing().iterator().next().getTarget()) {
										p.setContent(p.getContent()+x.getName()+"+");
										break;
									}
								}
							}
						}
						p.setContent(StringUtils.chop(p.getContent())+")");
						
					}else {
						while(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("Gateway")) {
							if(curr.getOutgoing().iterator().next().getTarget().getOutgoing().size()>1) {
								//TODO
							}else {
								curr = curr.getOutgoing().iterator().next().getTarget();
							}
						}
						if(!elem.contains(curr.getOutgoing().iterator().next().getTarget())) {
							while (containsProc(workInProgress, "P"+countcopy)) {
								countcopy++;
							
							}
							p.setContent(p.getContent()+"*P"+countcopy);
							mapping(modelInstance, curr.getOutgoing().iterator().next().getTarget(), count+1, countcopy);
							count++;
							countcopy++;
						}else {
							for(Prozess x : workInProgress) {
								if(x.getFirst()==curr.getOutgoing().iterator().next().getTarget()) {
									p.setContent(p.getContent()+"*"+x.getName());
									break;
								}
							}
						}
					}	
				}
				
				
				prozesse.add(p);
				workInProgress.add(p);
				//else if(curr.getOutgoing().iterator().next().getTarget().getClass().toString().contains("ParallelGateway")){}
		return prozesse;
		}
	
	
	
	
	
	
	
	
	
	
	
	//veraltet
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
