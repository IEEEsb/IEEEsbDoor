#include <avr/wdt.h>  
char incomingByte = 0;
bool receiving = false;
String incomingCommand = "";
volatile bool isOpen;


void checkOpen()
{
  if(digitalRead(2) == 0 && isOpen)
  {
    Serial.println("Puerta cerrada");
    isOpen = false;
  }
  else if(digitalRead(2) == 1 && !isOpen)
  {
    Serial.println("Puerta abierta");
    isOpen = true;
  }
}

void setup()  
{
  wdt_disable();  
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH);
  delay(100);
  attachInterrupt(0, checkOpen, CHANGE);
  Serial.begin(9600);
  pinMode(7, OUTPUT);
  pinMode(2, INPUT_PULLUP);
  isOpen = digitalRead(2);
  digitalWrite(13, LOW);
  wdt_enable(WDTO_2S);  
}

void loop() // run over and over
{
  if(Serial.available() > 0)
  {
    incomingByte = Serial.read();
    if(!receiving)
    {
      if(incomingByte=='-')
      {
        receiving = true;
      }
    } 
    else 
    {
      if(incomingByte=='-')
      {
        receiving = false;
        processCommand(incomingCommand);
        incomingCommand = "";
      } 
      else 
      {
        incomingCommand += incomingByte;
      }
    }
  }
  wdt_reset();  
}

void processCommand(String command) 
{
  Serial.flush();
  if(command.equals("ABRE") && !isOpen)
  {
    digitalWrite(7, HIGH);
    delay(1000);
    digitalWrite(7, LOW);
  }
}
