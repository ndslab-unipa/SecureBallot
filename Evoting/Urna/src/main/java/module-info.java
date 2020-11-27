module Urna {
	opens urna.view;
	
	exports urna.app;
	exports urna.model;
	exports urna.controller;
	exports urna.view;
	exports urna.view.viewmodel;

	requires transitive Common;
	
	requires java.desktop;
	requires java.sql;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
}