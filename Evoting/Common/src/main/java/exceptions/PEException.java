package exceptions;

public class PEException extends Exception {
	private static final long serialVersionUID = 1L;

	public enum Code {

		/*------------*
		 *-Connection-*
		 *------------*/

		CNN_0,	CNN_1,	CNN_2,	CNN_3,

		/*------------*
		 *-Encryption-*
		 *------------*/

		ENC_0,	ENC_1,	ENC_2,	ENC_3,

		ENC_4,	ENC_5,	ENC_6,	ENC_7,
		
		ENC_8,

		/*-------------*
		 *-Development-*
		 *-------------*/

		DEV_0,	DEV_1,	DEV_3,	DEV_4,

		DEV_5,	DEV_6,	DEV_7,	DEV_8,

		DEV_9, 	DEV_10,

		/*---------*
		 *-Failure-*
		 *---------*/

		FLR_0,	FLR_1, 	FLR_2,	FLR_3,

		FLR_4,	FLR_5, 	FLR_6,	FLR_7,

		FLR_8,	FLR_9,	FLR_10,	FLR_11,
		
		FLR_12, FLR_13, FLR_14, FLR_15,
		
		FLR_16,	FLR_17,	FLR_18, FLR_19,

		/*------*
		 *-Cast-*
		 *------*/

		CST_0, 	CST_1,	CST_2,

		/*----------*
		 *-Database-*
		 *----------*/

		DB_0,	DB_1,	DB_2,	DB_3,

		DB_4,	DB_5,	DB_6,	DB_7,

		DB_8,	DB_9,	DB_10,	DB_11,

		DB_12,	DB_13, 	DB_14,	DB_15
	}

	protected Code code;
	protected String generic;
	protected String specific;

	protected PEException(Code code, String generic, String specific, Exception cause){
		super(specific, cause);
		this.code = code;
		this.generic = generic;
		this.specific = specific;
	}

	public Code getCode() {
		return code;
	}

	public void setCode(Code code) {
		this.code = code;
	}

	public String getGeneric() {
		return generic;
	}

	public void setGeneric(String generic) {
		this.generic = generic;
	}

	public String getSpecific() {
		return specific;
	}

	public void setSpecific(String specific) {
		this.specific = specific;
	}
}
