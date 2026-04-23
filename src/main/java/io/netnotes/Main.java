package io.netnotes;

import java.util.concurrent.CompletionException;

import io.netnotes.system.SystemApplication;

public class Main {
    
    public static void main(String[] args) {
        try {
            SystemApplication.start().join();
            System.exit(0);
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            cause.printStackTrace();
            System.exit(1);
        }
    }

    
    
}


