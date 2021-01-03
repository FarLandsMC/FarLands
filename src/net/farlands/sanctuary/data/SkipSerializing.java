package net.farlands.sanctuary.data;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SkipSerializing { }
