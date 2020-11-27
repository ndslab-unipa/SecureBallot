package model;

import java.time.LocalDateTime;

//TODO: Aggiungere controlli sulle date
public class Session {
	private int code;
	private Procedure procedure;
	private LocalDateTime start, end;
	private boolean validity;
	
	public Session(int code, Procedure proc, LocalDateTime start, LocalDateTime end, boolean validity) {
		this.code = code;
		this.procedure = proc;
		this.start = start;
		this.end = end;
		this.validity = validity;
	}
	
	public Session(int code, int procCode, String procName, String procSupervisor, LocalDateTime start, LocalDateTime end, boolean validity) {
		this(code, new Procedure(procCode, procName, procSupervisor), start, end, validity);
	}

	public int getCode() { return code; }
	public void setCode(int code) { this.code = code; }
	
	public Procedure getProcedure() { return procedure; }
	public void setProcedure(Procedure procedure) { this.procedure = procedure; }
	
	public LocalDateTime getStart() { return start; }
	public void setStart(LocalDateTime start) { this.start = start; }
	
	public LocalDateTime getEnd() { return end; }
	public void setEnd(LocalDateTime end) {	this.end = end; }

	public boolean getValidity() { return validity; }
	public void setValidity(boolean validity) { this.validity = validity; }
	
	public int getProcedureCode() { return procedure.getCode(); }
	public String getProcedureName() { return procedure.getName(); }
	public String getProcedureSupervisor() { return procedure.getSupervisor(); }
	public LocalDateTime getProcedureStart() { return procedure.getStart(); }
	public LocalDateTime getProcedureEnd() { return procedure.getEnd(); }
	public boolean getProcedureValidity() { return procedure.getTerminated(); }
	
	@Override
	public String toString() {
		return "Session Code: "+code+"\n----\n"+procedure+"\n----\nStart: "+start+"\nEnd: "+end+"\nValid: "+validity;
	}

	//Aggiunto da Mk1092
	//Discuterne con Claud10R

	public Session(int code, LocalDateTime starts, LocalDateTime ends){
		this.code = code;
		this.start = starts;
		this.end = ends;
	}

	public boolean isChronological() {return start.isBefore(end);}

	public boolean inProcedure(LocalDateTime procedureStart, LocalDateTime procedureEnd) {
		return start.isAfter(procedureStart) && end.isBefore(procedureEnd);
	}

	/**
	 * Verifica se la Sessione è del tutto precedente a quella passata per argomento(1), del tutto successiva (0),
	 * o se le due sessioni sono in parte coincidenti (-1).
	 * @param other L'altra sessione.
	 * @return
	 */
	public int before(Session other) {

		if(end.isBefore(other.start)) {
			return 1;
		}

		if(start.isAfter(other.end)) {
			return 0;
		}

		return -1;
	}
}
