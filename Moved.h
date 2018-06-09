#ifndef Auto_Move_H_
#define Auto_Move_H_
#include "Arduino.h"

#define FORWARD  0x09
#define BACKWARD  0x06
#define LEFT_U  0x0A
#define RIGHT_U 0x05
#define LEFT    0x08
#define RIGHT   0x01
#define STOP    0x00

#ifdef __cplusplus
extern "C" {
#endif
void loop();
void setup();
#ifdef __cplusplus
}
#endif

void Motor_mode(int da);
void Motor_Control(char da, unsigned int OC_value);

#endif
