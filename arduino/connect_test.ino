#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"

#include <Wire.h>
#include <Adafruit_AMG88xx.h>

#include "BluefruitConfig.h"

Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);
Adafruit_AMG88xx amg;
float pixels[AMG88xx_PIXEL_ARRAY_SIZE];

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}


int32_t serviceId;
int32_t charId;

void setup() {
  // put your setup code here, to run once:
  while (!Serial);
  delay(500);

  boolean success;
  boolean status;

  Serial.begin(115200);
  Serial.println(F("Adafruit Bluefruit connection test"));
  Serial.println(F("------------------------------------------"));

  randomSeed(micros());

  Serial.print(F("Initializing the Bluefruit LE module: "));

  if (!ble.begin(VERBOSE_MODE))
  {
    error(F("Couldn't find the Bluefruit, make sure it is in Command Mode and check shit"));
  }

  Serial.println(F("OK"));

  Serial.println(F("Performing a factory reset: "));
  if (!ble.factoryReset()) {
    error(F("Couldn't factory reset"));
  }

  ble.echo(false);

  Serial.println("Requesting Bluefruit info: ");
  ble.info();

  Serial.println(F("Setting device name to 'IR recv': "));

  if (!ble.sendCommandCheckOK(F("AT+GAPDEVNAME=IR recv"))) {
    error(F("Could not set device name?"));
  }

  Serial.println(F("Adding the service def (UUID = 0x180D): "));
  success = ble.sendCommandWithIntReply(F("AT+GATTADDSERVICE=UUID=0x180D"), &serviceId);
  if (!success) {
    error(F("Could not add service"));
  }

  Serial.println(F("Adding the characterisitc (UUID = 0x2A37): "));
  success = ble.sendCommandWithIntReply(F("AT+GATTADDCHAR=UUID=0x2A37, PROPERTIES=0x12, MIN_LEN=2, MAX_LEN=3, VALUE=00-40"), &charId);

  if (!success) {
    error(F("Could not add characterisic"));
  }

  Serial.print(F("Performing a SW reset): "));
  ble.reset();

  Serial.println();

  status = amg.begin();
  if (!status) {
    Serial.println("Could not find a valid sensor, check wiring!");
    while (1);
  }

  Serial.println(F("-- Thermal senson ready to go -- "));

  delay(200);
}

void loop() {
  // put your main code here, to run repeatedly:
  amg.readPixels(pixels);

  int ir_temp = (getAverage(pixels, AMG88xx_PIXEL_ARRAY_SIZE) * (9/5)) + 32;


  Serial.print(F("Updating temp to "));
  Serial.print(ir_temp);
  Serial.println(F(" F"));

  ble.print(F("AT+GATTCHAR="));
  ble.print(charId);
  ble.print(F(",00-"));
  ble.println(ir_temp, HEX);

  if (!ble.waitForOK()) {
    Serial.println(F("Failed to get a response!"));
  }

  delay(2000);

}

int getAverage(float* arr, int len ) {
  int avg = 0;
  for (int i = 0; i < len; i++) {
    avg = avg + arr[i];
  }

  avg = avg / len;

  return avg;
}
