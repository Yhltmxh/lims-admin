package com.shou.lims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenPassword {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
