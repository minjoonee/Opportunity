#include "Moved.h"
//소리는 1초에 340m를 이동한다.
//초음파가 발사되고 돌아오는데 300us면 10.2cm가 계산됨. 장애물과의 거리는 5.1cm
//전방에 장애물이 30cm 이내로 존재할 경우, 30cm이내에 장애물이 감지되지 않는 방향으로 회전을 실시한다.
//방향을 찾으면 직진
char RX_buf[17]; // 초음파 센서의 데이터 수신하기 위한 버퍼
unsigned char RX_buf2[7]; // 전방 초음파 7개의 거리측정 값을 저장하기 위한 변수
unsigned char TX_buf0[5] = {0x76, 0x00, 0xAF, 0xE0, 0x8F}; // 초음파 센서중에 전방(F0~6)만 동작
unsigned char TX_bufs[5] = {0x76, 0x00, 0x0F, 0x00, 0x0F}; // 초음파 센서 동작 정지하기위한 패킷

int Motor[6] = {22,23,24,25,4,5};
int data = 0, flag = 0;
//data : 거리를 비교한 값을 저장
//flag : 전방에 장애물의 발견을 알리기 위한 값or회전후 동작시에 일정 내용을 무시하도록 하기 위한 값
int delay_time = 200,RX_flag = 1,Move_flag = 0;
//dealy_time : 회전할때 일정하게 돌게 하기 위한 값
//RX_flag : 버퍼에 수신되는 값을 처음 동작하거나 회전후 동작시에 일정 버퍼의 내용을 무시하기 위한 값
//Move_flag : SmartCar 동작 상태 값
int PWM_value1 = 240; // 회전시 속도제어
int PWM_value2 = 110; // 직진시 속도제어

void setup()
{
	int z;
	delay(1000);
	Serial1.begin(115200);
	Serial.begin(115200);
	for(z=0;z<6;z++)
	{
		pinMode(Motor[z],OUTPUT);
		digitalWrite(Motor[z],LOW);
	}
	Serial1.write(TX_buf0,5); // 전방에 초음파가 동작하도록 데이터를 전송
	Motor_Control('A',PWM_value2); // 모터 제어 속도를 PWM_value2로 설정 후 전진
	Motor_mode(STOP);
	Move_flag = 1; //현재 상태를 동작중으로 표시
	flag=0;
	Serial.println("Complete");
}

void loop()
{
	if(flag) // flag가 1인 경우 우선 모터를 정지한다.
	{
		Motor_mode(STOP);
		Serial.println("start loop-Motor Stop");
		//data는 거리를 비교한 값
		switch(data & 0x63)
		{
			case 0x01:
			case 0x02:
			case 0x03:
				Motor_Control('A',PWM_value1);
				Motor_mode(BACKWARD);
				Serial.println("Back!");
				delay(delay_time);
				Motor_mode(STOP);
				Serial.println(flag);
				break;
			case 0x20:
			case 0x40:
			case 0x60:
				Motor_Control('A',PWM_value1);
				Motor_mode(BACKWARD);
				Serial.println("Back!");
				delay(delay_time);
				Motor_mode(STOP);
				Serial.println(flag);
				break;
			//0x01,02,03, 20,40,60 delay_time(100)시간만큼 후진한다
			default:
				Motor_Control('A',PWM_value1);
				Motor_mode(BACKWARD);
				Serial.println("check sensor");
				delay(delay_time);
				Motor_mode(STOP);
				Serial.println(flag);
				break;
			//아무것도 아니면 두배의 시간만큼 좌측유턴
		}
		//실행이 완료되면
		Serial1.write(TX_buf0,5); // 초음파를 다시 동작시킨다.
		Motor_mode(STOP); //모터를 정지하고
		Serial.println("finish loop-Motor Stop");
		flag = 0; // flag를 0으로 만들어서 앞에 장애물이 없다고 판단한다.
	}
}

void serialEvent1()
{
	unsigned char z,tmp=0;
	Serial1.readBytes(RX_buf,17); //serial1의 버퍼에 수신된 데이터를 RX_buf에 저장
	if((RX_buf[0] == 0x76) && (RX_buf[1] == 0))
	{
		if(Move_flag) // 동작상태일때, 실행
		{
			for(z=2;z<16;z++)
				tmp += (unsigned char)RX_buf[z];
			tmp = tmp & 0xFF;
			if((unsigned char)RX_buf[16] == tmp)
			{

				for(z=0;z<7;z++)
					RX_buf2[z] = (unsigned char)RX_buf[z+4];
				//전방 초음파 센서값만 저장
				data = 0;
				for(z=0;z<7;z++)
				{
					if(RX_buf2[z] < 20) // 전방 초음파 7개의 값들이 10이하일 경우, data에 각각의 비트에 맞게 1로 만듬
						data |= 0x01<<z;
				}
				if((data & 0x1C) != 0) // 전방에 장애물이 있다는 의미
				{
					if(RX_flag==4)
					{
						flag = 1;
						Serial1.write(TX_bufs,5); //초음파 동작을 정지
						RX_flag = 0;
					}
				}
				if(RX_flag != 4)
				{
					RX_flag++;
					Serial.println(RX_flag);
				}
			}
		}
	}
	else
	{
		for(z=1;z<17;z++)
		{
			if(RX_buf[z]==0x76) //시작 패킷 데이터인 0x76,0x00을 찾는다
			{
				if(z!=16)
				{
					if(RX_buf[z+1]==0)
					{
						tmp = z;
						break;
					}
				}
				else // z가 16이면
				{
					tmp = z;
				}
			}
		}
		Serial1.readBytes(RX_buf,tmp);
	}
}

void serialEvent()
{
  unsigned char da=Serial.read();
  Serial.write(da);
  switch(da)
  {
    case 't': // t면 센서 동작
      Serial1.write(TX_buf0,5);// 각각의 배열을 UART1로 전송한다
      //Move_flag = 1;
      break;
    case 's': // s이면 센서 멈춤
      flag = 0;
      Move_flag = 0;
      Motor_mode(STOP);
      break;
    case 'f': // f이면 전진
		Motor_Control('A',PWM_value2-20); //전진 속도가 회전속도보다 두바퀴가 같이 돌아가서 빠르다
		Motor_mode(FORWARD); // 모터는 다시 전진을 시작한다.
		Move_flag=1;
	      flag = 0;
		break;
    case 'l': // l이면 좌회전
		Motor_Control('A',PWM_value2+30);
		Motor_mode(LEFT); // 모터는 다시 전진을 시작한다.
    	Move_flag=1;
        flag = 0;
		break;
    case 'r': //r이면 우회전
		Motor_Control('A',PWM_value2+50); // 우회전 모터가 힘이 좀 약함
		Motor_mode(RIGHT); // 모터는 다시 전진을 시작한다.
    	Move_flag=1;
        flag = 0;
		break;
    case 'b': //back일때
		Motor_Control('A',PWM_value2-20);
		Motor_mode(BACKWARD); // 모터는 다시 전진을 시작한다.
    	Move_flag=1;
        flag = 0;
		break;
    case '+': //+일때 속도 증가
    	if(PWM_value2<200) // 최고속도
    		PWM_value2+=5;
		Serial.println(PWM_value2);
		break;
    case '-'://-일때 속도 감소
    	if(PWM_value2>80) //최저 속도
    		PWM_value2-=5;
		Serial.println(PWM_value2);
    	break;
    case 'u'://u turn 일 때
		Motor_Control('A',PWM_value2);
		Motor_mode(LEFT_U); // 모터는 회전한다
    	Move_flag=1;
        flag = 0;
		break;
  }
  Serial.print("\n\r");
}

void Motor_mode(int da)
{
	int z;
	for(z=0;z<4;z++)
		digitalWrite(Motor[z],(da>>z) & 0x01);
}

void Motor_Control(char da, unsigned int OC_value)
{
	switch(da)
	{
		case 'L':
			analogWrite(Motor[4],OC_value);
			break;
		case 'R':
			analogWrite(Motor[5],OC_value);
			break;
		case 'A':
			analogWrite(Motor[4],OC_value);
			analogWrite(Motor[5],OC_value);
			break;
	}
}
