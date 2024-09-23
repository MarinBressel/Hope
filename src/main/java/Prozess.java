import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class Prozess {
	private String name;
	private String content;
	private FlowNode first;

	
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public FlowNode getFirst() {
		return first;
	}

	public void setFirst(FlowNode first) {
		this.first = first;
	}
}

	