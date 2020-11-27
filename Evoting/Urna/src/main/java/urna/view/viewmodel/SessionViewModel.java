package urna.view.viewmodel;

import java.time.LocalDateTime;

import model.Session;

public class SessionViewModel extends Session {
	private boolean selected = false;
	
	public SessionViewModel(int code, int procCode, String procName, String procSupervisor, LocalDateTime start, LocalDateTime end, boolean validity) {
		super(code, procCode, procName, procSupervisor, start, end, validity);
	}
	
	public SessionViewModel(Session s) {
		super(s.getCode(), s.getProcedure(), s.getStart(), s.getEnd(), s.getValidity());
	}
	
	public boolean isSelected() { return selected; }
	public void setSelected(boolean x) { selected = x; }
}
