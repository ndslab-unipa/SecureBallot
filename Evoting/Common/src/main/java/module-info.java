module Common {
	exports controller;
	exports encryption;
	exports exceptions;
	exports model;
	exports utils;
	exports view;
	exports view.viewmodel;
	exports db;
	exports rfid;

    requires org.bouncycastle.provider;
	requires commons.lang3;
	
	requires java.desktop;
	requires java.sql;
	requires jdk.unsupported;
	
	requires transitive rxtx;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
}