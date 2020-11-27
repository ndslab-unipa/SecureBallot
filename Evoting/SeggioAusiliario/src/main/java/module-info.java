module SeggioAusiliario {
	opens seggio.aux.view;
	
	exports seggio.aux.app;
	exports seggio.aux.model;
	exports seggio.aux.view;
	exports seggio.aux.controller;
	exports seggio.aux.view.viewmodel;

	requires transitive Common;
	
	//requires java.desktop;

	requires javafx.fxml;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
}