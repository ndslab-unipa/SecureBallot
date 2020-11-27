package model;

import java.time.LocalDateTime;

import exceptions.FLRException;
import exceptions.PEException;

//TODO: Aggiungere controlli sulle date
public class Procedure {
	protected int code;
	protected String name, supervisor;
	protected LocalDateTime start, end;
	protected boolean terminated;
	
	public Procedure(int code, String name, String supervisor) {
		this.code = code;
		this.name = name;
		this.supervisor = supervisor;
	}
	
	public Procedure(int code, String name, String supervisor, LocalDateTime start, LocalDateTime end, boolean terminated) {
		this(code,name,supervisor);
		this.start = start;
		this.end = end;
		this.terminated = terminated;
	}

	public Procedure(int code, String name, LocalDateTime start, LocalDateTime end, String supervisor) throws PEException {
		if(end.isBefore(start)){
			throw FLRException.FLR_19();
		}

		this.code = code;
		this.name = name;
		this.start = start;
		this.end = end;
		this.supervisor = supervisor;
	}
	
	public int getCode() { return code; }
	public void setCode(int code) { this.code = code; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public String getSupervisor() { return supervisor; }
	public void setSupervisor(String supervisor) { this.supervisor = supervisor; }
	
	public LocalDateTime getStart() { return start; }
	public void setStart(LocalDateTime start) { this.start = start; }
	
	public LocalDateTime getEnd() { return end; }
	public void setEnd(LocalDateTime end) { this.end = end; }
	
	public boolean getTerminated() { return terminated; }
	public void setTerminated(boolean terminated) { this.terminated = terminated; }
	
	@Override
	public String toString() {
		return "Codice Procedura: "+code+"\nNome: "+name+"\nSupervisore: "+supervisor+"\nInizio: "+start+"\nFine: "+end;
	}
}
