import org.camunda.bpm.model.bpmn.instance.FlowNode;

public class ACT {
	private FlowNode akt;
	private String anweis;
	
	public ACT(FlowNode akti, String anweisung){
		akt = akti;
		anweis = anweisung;
	}
	
	public FlowNode getAkt() {
		return akt;
	}
	public void setAkt(FlowNode akt) {
		this.akt = akt;
	}
	public String getAnweisung() {
		return anweis;
	}
	public void setAnweisung(String anweisung) {
		this.anweis = anweisung;
	}
}
