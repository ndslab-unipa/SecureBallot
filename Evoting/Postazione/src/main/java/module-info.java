module Postazione {
	opens postazione.view;
	
	exports postazione.app;
	exports postazione.model;
	exports postazione.view;
	exports postazione.controller;
	exports postazione.view.viewmodel;

	requires transitive Common;
	
	requires java.desktop;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
}