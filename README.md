# IoT-Android
A WIP proof-of concept Android app that uses Internet of Things Arduino app to send environmental temperature alerts

## Overview

This is an Android app (very much work in progress) in which demonstrates the use of Internet-of-things technology by combining the Bluetooth capabilites of Android with that of Arduino.

### The Arduino Portion
Connected to the Arduino is a infrared temperature sensor, which is able to read temperature as a single value or as a 8x8 matrix of values - allowing for a more accurate reading of the target area.
There is also a bluetooth module connected which relays the information of the ir sensor over a bluetooth connection to the Android app.

### The Android Portion
There is a BLE (Bluetooth Low Energy) app created on the Android which looks for the bluetooth module broadcast from the Arduino system, receives the temperature data, and processes it accordingly (i.e., show alerts if temperature breaks threshold).
