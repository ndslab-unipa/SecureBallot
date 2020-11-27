module Seggio {
	opens seggio.view;
	
	exports seggio.app;
	exports seggio.model;
	exports seggio.view;
	exports seggio.controller;
	exports seggio.view.viewmodel;

	requires transitive Common;
	
	requires java.desktop;
	requires java.base;
	
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
}