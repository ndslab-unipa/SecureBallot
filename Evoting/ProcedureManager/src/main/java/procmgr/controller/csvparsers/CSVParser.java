package procmgr.controller.csvparsers;

import procmgr.controller.Controller;
import procmgr.model.ProcedurePM;

/**
 * Classe base da cui derivano tutti i parser di file CSV, per i vari file necessari alla creazione di una procedura elettorale. Utile
 * per far avere a tutte le classi figlie attributi per controller e procedura.
 */
public class CSVParser {
	protected static Controller controller;
	protected static ProcedurePM newProcedure;
	
	protected static final String fieldSeparator = ";";
	
	protected static void initControllerAndProcedure(Controller c, ProcedurePM p) {
		controller = c;
		newProcedure = p;
	}
}
