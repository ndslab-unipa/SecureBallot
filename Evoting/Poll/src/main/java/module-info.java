module Poll {
	opens poll.view;
	
	exports poll.app;
	exports poll.model;
	exports poll.view;
	exports poll.controller;
	exports poll.view.viewmodel;

	requires transitive Common;
	
	requires java.sql;
	requires java.desktop;
	requires java.base;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	
	requires org.apache.pdfbox;
}