# spriced-framework-flink

Run 
 - docker-compose build
 - docker-compose up

Then run the main method which will start a server in your local running in port 1108

Then go to localhost:38080 to open kafka-ui and send a sample message to user-logins topic 
    Sample message: key-1, value-{"user_id": "1", "user_name": "Test User", "login_type": "WEB"}

You will see a message coming in the greetings topic
