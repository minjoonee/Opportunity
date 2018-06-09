#include "Moved.h"
//�Ҹ��� 1�ʿ� 340m�� �̵��Ѵ�.
//�����İ� �߻�ǰ� ���ƿ��µ� 300us�� 10.2cm�� ����. ��ֹ����� �Ÿ��� 5.1cm
//���濡 ��ֹ��� 30cm �̳��� ������ ���, 30cm�̳��� ��ֹ��� �������� �ʴ� �������� ȸ���� �ǽ��Ѵ�.
//������ ã���� ����
char RX_buf[17]; // ������ ������ ������ �����ϱ� ���� ����
unsigned char RX_buf2[7]; // ���� ������ 7���� �Ÿ����� ���� �����ϱ� ���� ����
unsigned char TX_buf0[5] = {0x76, 0x00, 0xAF, 0xE0, 0x8F}; // ������ �����߿� ����(F0~6)�� ����
unsigned char TX_bufs[5] = {0x76, 0x00, 0x0F, 0x00, 0x0F}; // ������ ���� ���� �����ϱ����� ��Ŷ

int Motor[6] = {22,23,24,25,4,5};
int data = 0, flag = 0;
//data : �Ÿ��� ���� ���� ����
//flag : ���濡 ��ֹ��� �߰��� �˸��� ���� ��orȸ���� ���۽ÿ� ���� ������ �����ϵ��� �ϱ� ���� ��
int delay_time = 200,RX_flag = 1,Move_flag = 0;
//dealy_time : ȸ���Ҷ� �����ϰ� ���� �ϱ� ���� ��
//RX_flag : ���ۿ� ���ŵǴ� ���� ó�� �����ϰų� ȸ���� ���۽ÿ� ���� ������ ������ �����ϱ� ���� ��
//Move_flag : SmartCar ���� ���� ��
int PWM_value1 = 240; // ȸ���� �ӵ�����
int PWM_value2 = 110; // ������ �ӵ�����

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
	Serial1.write(TX_buf0,5); // ���濡 �����İ� �����ϵ��� �����͸� ����
	Motor_Control('A',PWM_value2); // ���� ���� �ӵ��� PWM_value2�� ���� �� ����
	Motor_mode(STOP);
	Move_flag = 1; //���� ���¸� ���������� ǥ��
	flag=0;
	Serial.println("Complete");
}

void loop()
{
	if(flag) // flag�� 1�� ��� �켱 ���͸� �����Ѵ�.
	{
		Motor_mode(STOP);
		Serial.println("start loop-Motor Stop");
		//data�� �Ÿ��� ���� ��
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
			//0x01,02,03, 20,40,60 delay_time(100)�ð���ŭ �����Ѵ�
			default:
				Motor_Control('A',PWM_value1);
				Motor_mode(BACKWARD);
				Serial.println("check sensor");
				delay(delay_time);
				Motor_mode(STOP);
				Serial.println(flag);
				break;
			//�ƹ��͵� �ƴϸ� �ι��� �ð���ŭ ��������
		}
		//������ �Ϸ�Ǹ�
		Serial1.write(TX_buf0,5); // �����ĸ� �ٽ� ���۽�Ų��.
		Motor_mode(STOP); //���͸� �����ϰ�
		Serial.println("finish loop-Motor Stop");
		flag = 0; // flag�� 0���� ���� �տ� ��ֹ��� ���ٰ� �Ǵ��Ѵ�.
	}
}

void serialEvent1()
{
	unsigned char z,tmp=0;
	Serial1.readBytes(RX_buf,17); //serial1�� ���ۿ� ���ŵ� �����͸� RX_buf�� ����
	if((RX_buf[0] == 0x76) && (RX_buf[1] == 0))
	{
		if(Move_flag) // ���ۻ����϶�, ����
		{
			for(z=2;z<16;z++)
				tmp += (unsigned char)RX_buf[z];
			tmp = tmp & 0xFF;
			if((unsigned char)RX_buf[16] == tmp)
			{

				for(z=0;z<7;z++)
					RX_buf2[z] = (unsigned char)RX_buf[z+4];
				//���� ������ �������� ����
				data = 0;
				for(z=0;z<7;z++)
				{
					if(RX_buf2[z] < 20) // ���� ������ 7���� ������ 10������ ���, data�� ������ ��Ʈ�� �°� 1�� ����
						data |= 0x01<<z;
				}
				if((data & 0x1C) != 0) // ���濡 ��ֹ��� �ִٴ� �ǹ�
				{
					if(RX_flag==4)
					{
						flag = 1;
						Serial1.write(TX_bufs,5); //������ ������ ����
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
			if(RX_buf[z]==0x76) //���� ��Ŷ �������� 0x76,0x00�� ã�´�
			{
				if(z!=16)
				{
					if(RX_buf[z+1]==0)
					{
						tmp = z;
						break;
					}
				}
				else // z�� 16�̸�
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
    case 't': // t�� ���� ����
      Serial1.write(TX_buf0,5);// ������ �迭�� UART1�� �����Ѵ�
      //Move_flag = 1;
      break;
    case 's': // s�̸� ���� ����
      flag = 0;
      Move_flag = 0;
      Motor_mode(STOP);
      break;
    case 'f': // f�̸� ����
		Motor_Control('A',PWM_value2-20); //���� �ӵ��� ȸ���ӵ����� �ι����� ���� ���ư��� ������
		Motor_mode(FORWARD); // ���ʹ� �ٽ� ������ �����Ѵ�.
		Move_flag=1;
	      flag = 0;
		break;
    case 'l': // l�̸� ��ȸ��
		Motor_Control('A',PWM_value2+30);
		Motor_mode(LEFT); // ���ʹ� �ٽ� ������ �����Ѵ�.
    	Move_flag=1;
        flag = 0;
		break;
    case 'r': //r�̸� ��ȸ��
		Motor_Control('A',PWM_value2+50); // ��ȸ�� ���Ͱ� ���� �� ����
		Motor_mode(RIGHT); // ���ʹ� �ٽ� ������ �����Ѵ�.
    	Move_flag=1;
        flag = 0;
		break;
    case 'b': //back�϶�
		Motor_Control('A',PWM_value2-20);
		Motor_mode(BACKWARD); // ���ʹ� �ٽ� ������ �����Ѵ�.
    	Move_flag=1;
        flag = 0;
		break;
    case '+': //+�϶� �ӵ� ����
    	if(PWM_value2<200) // �ְ�ӵ�
    		PWM_value2+=5;
		Serial.println(PWM_value2);
		break;
    case '-'://-�϶� �ӵ� ����
    	if(PWM_value2>80) //���� �ӵ�
    		PWM_value2-=5;
		Serial.println(PWM_value2);
    	break;
    case 'u'://u turn �� ��
		Motor_Control('A',PWM_value2);
		Motor_mode(LEFT_U); // ���ʹ� ȸ���Ѵ�
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
