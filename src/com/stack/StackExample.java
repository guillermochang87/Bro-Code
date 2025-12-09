package com.stack;

import java.util.Stack;

public class StackExample {

    public static void main(String[] args) {

        Stack<String> stack = new Stack<>();

        
        System.out.println("Is Stack empty?: " + stack.isEmpty());

        stack.push("Skyrim");
        stack.push("Doom");
        stack.push("Borderlands");

        System.out.println(stack);
        System.out.println("Is Stack empty?: " + stack.isEmpty());

        // stack.pop();
        // stack.pop();

        // Select without eliminate
        String favoriteGame = stack.peek();

        System.out.println(favoriteGame);
        System.out.println(stack);

        System.out.println("The position of the firts game is: " + stack.search("Borderlands"));

        

    }

}
