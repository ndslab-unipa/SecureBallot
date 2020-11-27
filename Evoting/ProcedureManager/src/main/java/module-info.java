module ProcedureManager {
	opens procmgr.view;
	
	exports procmgr.app;
	exports procmgr.model;
	exports procmgr.view;
	exports procmgr.controller;

	requires transitive Common;
	
	requires java.desktop;
	requires java.sql;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
}