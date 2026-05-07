🚀 PPE Compliance Monitoring System

 This project is an AI-based safety monitoring system designed to detect whether a person is wearing proper Personal Protective Equipment (PPE) such as a helmet and safety vest. It combines computer vision using YOLOv8 with a Java-based desktop application to generate real-time compliance results and store them in a MySQL database. The system is built using Java (Swing) for the user interface, Python for object detection, JDBC for database connectivity, and OpenCV for image and video processing. 

The application detects objects such as person, helmet, and safety vest, and generates a compliance result categorized as SAFE, WARNING, or UNSAFE. It supports both image and video input and maintains records of detection results in the database. Instead of relying only on confidence scores, a rule-based approach is used to improve reliability, where helmet and vest together result in SAFE, only one results in WARNING, and absence of both results in UNSAFE, while no person detected gives NO PERSON.

The system workflow involves the user interacting with the Java UI, which triggers Python scripts for detection using YOLO, processes the results, and stores them in the database. During development, key challenges included mismatch between UI and database outputs, which was resolved by standardizing the output format, and handling unreliable confidence values, which was improved using count-based logic. Another important aspect was integrating Java and Python, achieved through process execution and output parsing.

This project demonstrates practical implementation of AI with software development and can be further improved by enhancing detection accuracy, adding real-time webcam or CCTV support, and deploying it as a web-based system.

<img width="650" height="508" alt="img1_result" src="https://github.com/user-attachments/assets/c0bd506e-3f2e-490f-b28e-e4971a55dfa8" />

<img width="414" height="602" alt="img2_result" src="https://github.com/user-attachments/assets/329cf88d-56a9-45c9-a640-3b43159c694e" />

<img width="402" height="548" alt="img4_result" src="https://github.com/user-attachments/assets/fefb2172-4670-46d5-a9da-d513912edb90" />

