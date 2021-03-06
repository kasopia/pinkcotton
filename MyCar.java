import java.security.Principal;

import com.sun.corba.se.spi.servicecontext.SendingContextServiceContext;

import DrivingInterface.*;

public class MyCar {

    boolean is_debug = false;
    boolean is_accident = false; //사고여부
    int accident_count  = 0;
    int recovery_count = 0;

    public void control_driving(boolean a1, float a2, float a3, float a4, float a5, float a6, float a7, float a8,
                                float[] a9, float[] a10, float[] a11, float[] a12) {

        // ===========================================================
        // Don't remove this area. ===================================
        // ===========================================================
        DrivingInterface di = new DrivingInterface();
        DrivingInterface.CarStateValues sensing_info = di.get_car_state(a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12);
        // ===========================================================

        if(is_debug) {
            System.out.println("=========================================================");
            System.out.println("[MyCar] to middle: " + sensing_info.to_middle);

            System.out.println("[MyCar] collided: " + sensing_info.collided);
            System.out.println("[MyCar] car speed: " + sensing_info.speed + "km/h");

            System.out.println("[MyCar] is moving forward: " + sensing_info.moving_forward);
            System.out.println("[MyCar] moving angle: " + sensing_info.moving_angle);
            System.out.println("[MyCar] lap_progress: " + sensing_info.lap_progress);

            StringBuilder forward_angles = new StringBuilder("[MyCar] track_forward_angles: ");
            for (Float track_forward_angle : sensing_info.track_forward_angles) {
                forward_angles.append(track_forward_angle).append(", ");
            }
            System.out.println(forward_angles);

            StringBuilder to_way_points = new StringBuilder("[MyCar] distance_to_way_points: ");
            for (Float distance_to_way_point : sensing_info.distance_to_way_points) {
                to_way_points.append(distance_to_way_point).append(", ");
            }
            System.out.println(to_way_points);

            StringBuilder forward_obstacles = new StringBuilder("[MyCar] track_forward_obstacles: ");
            for (DrivingInterface.ObstaclesInfo track_forward_obstacle : sensing_info.track_forward_obstacles) {
                forward_obstacles.append("{dist:").append(track_forward_obstacle.dist)
                        .append(", to_middle:").append(track_forward_obstacle.to_middle).append("}, ");
            }
            System.out.println(forward_obstacles);

            StringBuilder opponent_cars = new StringBuilder("[MyCar] opponent_cars_info: ");
            for (DrivingInterface.CarsInfo carsInfo : sensing_info.opponent_cars_info) {
                opponent_cars.append("{dist:").append(carsInfo.dist)
                        .append(", to_middle:").append(carsInfo.to_middle)
                        .append(", speed:").append(carsInfo.speed).append("km/h}, ");
            }
            System.out.println(opponent_cars);

            System.out.println("=========================================================");
        }

        // ===========================================================
        // Area for writing code about driving rule ==================
        // ===========================================================
        // Editing area starts from here
        //
        float set_brake = 0.0f;
		float set_throttle =2.6f;// 0.83f
		float set_streering = 0.0f;

		if ((sensing_info.lap_progress > 32f && sensing_info.lap_progress < 37f) 
				|| (sensing_info.lap_progress > 82f && sensing_info.lap_progress < 87f)){
            set_streering = -0.08f;
            set_throttle -= 0.05f;
        }
		
		int angle_num = 0;
		int ang = 80;

		if (sensing_info.speed > 200) {
			angle_num = 4;
			ang = 20;
		} else if (sensing_info.speed > 180) {
			angle_num = 3;
			ang = 25;
		} else if (sensing_info.speed > 160) {
			angle_num = 2;
			ang = 50;
		} else if (sensing_info.speed > 140) {
			angle_num = 2;
			ang = 80;
		} else if (sensing_info.speed > 110) {
			angle_num = 1;
			ang = 130;
		} else if (sensing_info.speed > 90) {
			angle_num = 1;
			ang = 150;
		} else if (sensing_info.speed > 70) {
			ang = 160;
		} else {
			ang = 180;
		}

		float ref_angle = sensing_info.track_forward_angles.get(angle_num);

		if (Math.abs(sensing_info.track_forward_angles.get(angle_num)) > 1 || Math.abs(sensing_info.moving_angle) > 0.5)
			set_streering = (sensing_info.track_forward_angles.get(angle_num) - (sensing_info.moving_angle)) / ang;
		else if(sensing_info.speed > 150 
				&& Math.abs(sensing_info.track_forward_angles.get(angle_num)) < 1 
				&& Math.abs(sensing_info.moving_angle) > 3) {
			set_throttle -= 0.15f;
			set_brake=0.01f;
		}

		// 도로 밖으로 나갔을 경우
		if (Math.abs(sensing_info.to_middle) > Math.abs(sensing_info.half_road_limit)-0.5) {
			// 도로 밖인데 장애물이 앞에있을경우
			if (sensing_info.track_forward_obstacles.size() != 0) {
				if (sensing_info.to_middle > 0) {
					set_streering -= 0.15f;
				} else {
					set_streering += 0.15f;
				}
			} else {
				if (sensing_info.to_middle > 0) {
					set_streering -= 0.1f;
				} else {
					set_streering += 0.1f;
				}
			}

			if (sensing_info.speed > 140)
				set_brake = 0.05f;
			// 도로 안일때
		} else if(Math.abs(sensing_info.to_middle) <= Math.abs(sensing_info.half_road_limit)-0.5 
				&&Math.abs(sensing_info.to_middle) > Math.abs(sensing_info.half_road_limit)-2) {
			if (sensing_info.to_middle > 0) {
				set_streering -= 0.15f;
			} else {
				set_streering += 0.15f;
			}
			
		} else if(Math.abs(sensing_info.to_middle) > 10) {
			//너무 많이 나갔을때 
			if (sensing_info.to_middle > 0) {
				set_streering -= 0.2f;
			} else {
				set_streering += 0.2f;
			}
			set_brake=0.1f;
		}
		
		int obsVal=80;
		
		if(sensing_info.speed > 150){
			obsVal=100;
			set_brake=0.1f;
		}else if(sensing_info.speed > 110){
			obsVal=80;
			set_brake=0.01f;
		}else if(sensing_info.speed > 90){
			obsVal=70;
		}else if(sensing_info.speed > 50){
			obsVal=50;
		}
		// 전방의 커브를 체크하여 급커브인경우 속도 감소 로직
		boolean full_throttle = true;
		boolean emergency_brake = false;
		
		// 전방의 장애물이 있을때만 처리
		if (Math.abs(sensing_info.track_forward_obstacles.size()) > 0) {
			for (int j=0 ; j<sensing_info.track_forward_obstacles.size();j++){
			// 가장 앞에 있는 장애물을 가져옴
			DrivingInterface.ObstaclesInfo fwd_obstacle = sensing_info.track_forward_obstacles.get(j);
			// 거리를 체크, 0~60m 앞에 있는 장애물만 처리, 도로밖에 있는 장애물 제외 처리
				// 차와 장애물 거리의 width
				//float avoid_width = 2.27f;
				float avoid_width = 2.7f;
				// 내 차량의 위치에서 장애물 위치를 빼준다
				float diff = fwd_obstacle.to_middle - sensing_info.to_middle;
				//ref_angle = (float) Math.abs(((Math.atan(diff - avoid_width)+sensing_info.moving_angle)/ fwd_obstacle.dist) * 57.29579);
				ref_angle = (float) Math.abs((Math.atan(diff - avoid_width)/ fwd_obstacle.dist) * 57.29579);
				// 차량 전방을 장애물이 막고있을때			
				
				if (fwd_obstacle.dist <= obsVal && fwd_obstacle.dist > 5 && Math.abs(fwd_obstacle.to_middle) < Math.abs(sensing_info.half_road_limit)-1.5 ) {
		
					if (Math.abs(diff) < avoid_width) {
						
						if(sensing_info.speed > 150){
							set_brake = 0.1f;
						}else {
							set_brake = 0.02f;
						}
						
						if (diff > 0)	ref_angle *= -1;
						
						//장애물이 - 있으면 + / 장애물이 + 이면 -  
						
						if(sensing_info.speed > 160) {
							set_streering += ref_angle / 100;
						}else if(sensing_info.speed > 120) {
							set_streering += ref_angle / 80;
						}else{
							set_streering += ref_angle / 40;
						}
					}
					
				}else if(fwd_obstacle.dist < 5 && Math.abs(diff) < avoid_width){
					emergency_brake = true;
				}
			}
		}


		// 급 커브 처리
		for (int i = 0; i < sensing_info.track_forward_angles.size(); i++) {
			float fwd_angle = sensing_info.track_forward_angles.get(i) + sensing_info.moving_forward;
			fwd_angle = Math.abs(fwd_angle);
			
			if ((fwd_angle > 30 && sensing_info.speed > 180)) {
				set_throttle -= 0.25f;
				set_brake = 0.18f;
			}
			
			if ((fwd_angle > 30 && sensing_info.speed > 150)) {
				set_throttle -= 0.15f;
				set_brake = 0.18f;
			}

			if ((fwd_angle > 30 && sensing_info.speed > 120)) {
				//set_throttle = 0.96f;
				set_throttle -= 0.05f;
				set_brake = 0.13f;
			}

			if ((fwd_angle > 50 && sensing_info.speed > 100)) {
				set_throttle -= 0.05f;
				set_brake = 0.10f;
			}
			

			if (fwd_angle > 80 && sensing_info.speed > 100) {
				full_throttle = false;
			}

			if (fwd_angle > 120 && sensing_info.speed > 60 && i<6) {
				emergency_brake = true;
				break;
			}
			
			if(i> 7 && Math.abs(sensing_info.track_forward_angles.get(i)) <1) {
				set_brake = 0f;
			}
		}


		// 급정거 필요시
		if (!full_throttle) {
			// 속도를 줄여준다 default 130, 120
			if(sensing_info.speed > 200){
				set_throttle -= 1.2f;
			}
			
			if(sensing_info.speed > 150){
				set_throttle -= 1.0f;
			}

			if(sensing_info.speed > 130){
				set_throttle -= 0.4f;
			}

			if(sensing_info.speed > 120){
				set_brake = 0.8f;
			}
			if(sensing_info.speed > 100){
				set_brake  = 1.0f;
			}
		}

		// 유턴 및 특정 커브 필요시 핸들 값을 계산된 것에 더해서 더많이 핸들을 돌리도록 설정
		if (emergency_brake) {
			if (set_streering > 0) {
				set_streering += 0.3f;
			} else {
				set_streering -= 0.3f;
			}

			set_throttle -= 2.0f;
		}

		if (sensing_info.lap_progress > 0.5 && !is_accident 
				&& (sensing_info.speed < 1.0 && sensing_info.speed > -1)) {
			accident_count++;
		}

		// 6이상일 경우 사고
		if (accident_count > 6) {
			is_accident = true;
		}
		if (is_accident) {
			// 사고일 경우 왼쪽으로 틀기 0.02f
			set_streering = 0.03f;
			set_brake = 0;
			set_throttle = -1;
			// 후진하기위해 사용
			recovery_count++;
		}

		// default 20
		if (recovery_count > 15) {
			// 후진완료후 변수 초기화
			is_accident = false;
			recovery_count = 0;
			accident_count = 0;
			set_streering = 0;
			set_brake = 0;
			set_throttle = 0.98f;
		}

        
        // Moving straight forward
        car_controls.steering = set_streering;
        car_controls.throttle = set_throttle;
        car_controls.brake = set_brake;

        if(is_debug) {
            System.out.println("[MyCar] steering:"+car_controls.steering+
                                     ", throttle:"+car_controls.throttle+", brake:"+car_controls.brake);
        }

        //
        // Editing area ends
        // =======================================================
    }

    // ===========================================================
    // Don't remove below area. ==================================
    // ===========================================================
    public native void StartDriving();

    static MyCar car_controls;

    float throttle;
    float steering;
    float brake;

    static {
        System.loadLibrary("DrivingInterface/DrivingInterface");
    }

    public static void main(String[] args) {
        car_controls = new MyCar();
        car_controls.StartDriving();
    }
    // ===========================================================
}
