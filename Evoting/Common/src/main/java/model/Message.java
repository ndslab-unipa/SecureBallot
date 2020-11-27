package model;

import java.util.ArrayList;
import java.util.HashMap;

import exceptions.FLRException;
import exceptions.PEException;

public class Message extends Parsable {
	private static final long serialVersionUID = 1L;

	private String value = null;
	private final ArrayList<String> errors = new ArrayList<>();
	
	private final HashMap <String, Object> map;
	
	public Message() {
		map = new HashMap <>();
	}
	
	public Message(String value) {
		this.value = value;
		map = new HashMap <>();
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void addError(String error) {
		this.errors.add(error);
	}
	
	public ArrayList<String> getErrors(){
		return errors;
	}
	
	public void verifyMessage(String expectedValue, String[] required, Class<?>[] types, String sender) throws PEException {
		if(!errors.isEmpty()) {
			throw FLRException.FLR_08(sender, errors);
		}
		
		if(value == null) {
			throw FLRException.FLR_09(sender, expectedValue, "void");
		}
		
		if(!expectedValue.equals(value)) {
			throw FLRException.FLR_09(sender, expectedValue, value);
		}
		
		if(required == null) {
			return;
		}
		
		if(types == null || required.length != types.length) {
			throw FLRException.FLR_0(null);
		}
		
		ArrayList<String> missingElements = new ArrayList<>();
		ArrayList<String> wrongTypeElements = new ArrayList<>();
		
		for (int i = 0; i < required.length; i++) {
			Object obj = map.get(required[i]);
			
			if (obj == null)
				missingElements.add(required[i]);
			else
				try {
					types[i].cast(obj);
				}
				catch (ClassCastException cce) {
					wrongTypeElements.add(required[i]);
				}
		}
		
		if(!missingElements.isEmpty() || !wrongTypeElements.isEmpty())
			throw FLRException.FLR_10(sender, missingElements, wrongTypeElements);
	}
	
	public void setElement(String name, Object object) {
		map.put(name, object);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getElement(String name) {
		return (T) map.get(name);
	}

	public String getValue() {
		return value;
	}
}
