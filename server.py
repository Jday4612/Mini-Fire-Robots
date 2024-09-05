import cv2
import socket
import numpy as np

def recvall(sock, count):
    buf = b''
    while count:
        newbuf = sock.recv(count)
        if not newbuf:
            return None
        buf += newbuf
        count -= len(newbuf)
    return buf

class VideoCapture:
    def __init__(self):
        self.host = "server ip"
        self.port = server port # 서버 Port Number
        self.current_frame = None  # 현재 프레임을 저장할 변수
        self.setup_socket()
        self.update()

    def setup_socket(self):
        # 서버 소켓 생성
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(10)
        print(f"서버가 {self.host}:{self.port}에서 대기 중입니다...")
        # 클라이언트 연결 대기
        self.client_socket, self.client_address = self.server_socket.accept()
        print(f"클라이언트 {self.client_address}가 연결되었습니다.")
        # 클라이언트로부터 초기 메시지 수신

        response = f"서버 ({self.host})와 연결되었습니다."
        self.client_socket.send(response.encode("utf-8"))

    def update(self):
        while True:
            length = recvall(self.client_socket, 16)
            if length:
                byteData = recvall(self.client_socket, int(length))
                data = np.frombuffer(byteData, dtype='uint8')

                frame = cv2.imdecode(data, cv2.IMREAD_COLOR)
                cv2.imshow('CAM', frame)

            if cv2.waitKey(1) & 0xFF == ord('q'):
                self.__del__()
                break

    def __del__(self):
        self.client_socket.close()
        self.server_socket.close()

video_capture = VideoCapture()
