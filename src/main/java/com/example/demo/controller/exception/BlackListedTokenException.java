package com.example.demo.controller.exception;

public class BlackListedTokenException extends Exception{
    public BlackListedTokenException(String message){
        super(message);
    }
}
