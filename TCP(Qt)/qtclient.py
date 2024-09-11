import socket
import cv2
import numpy as np

# 서버 설정
server_address = "서버IP"  # 서버의 IP로 변경
server_port = 5000

# 서버 연결
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((server_address, server_port))

# 응답 받기
response = client_socket.recv(1024).decode("utf-8")
print(f"{response}\n")

# webcam
cap = cv2.VideoCapture(0)

cap.set(3, 640)  # 가로 해상도
cap.set(4, 480)  # 세로 해상도

encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]

while True:
    try:
        ret, frame = cap.read()
        if not ret:
            break
        
        # 이미지를 JPEG 형식으로 인코딩
        result, frame = cv2.imencode('.jpg', frame, encode_param)
        data = np.array(frame)
        byteData = data.tobytes()
        
        # 이미지 데이터의 크기를 먼저 전송하고 그 뒤에 이미지 데이터 전송
        client_socket.sendall((str(len(byteData))).encode().ljust(16) + byteData)
        
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    except Exception as e:
        print(f"[연결 종료] {e}")
        break

cap.release()

# 소켓 닫기
client_socket.close()
