import datetime
import json
import pika
import time

# producer.py
# Script to load dummy bounce messages to rabbitMQ syringe app for testing purposes

# Config values
RABBIT_HOST = 'rabbitmq'
QUEUE_TOPIC = 'bounce'

connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBIT_HOST, virtual_host='test'))
channel = connection.channel()

# Note, this is just a dummy timestamp, not necessarily the true format
ts = time.time()
st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
data = {
    "envelope": "foo",
    "recipient": "bar",
    "x-tm-id": "123",
    "results": [
        {
            "description": "baz",
            "status": "qux",
            "code": 456,
            "time": st,
            "from": "corge",
            "state": "grault"
        }
    ]
}

message = json.dumps(data)
channel.basic_publish(exchange='', routing_key=QUEUE_TOPIC, body=message)

print(" [x] Sent data to RabbitMQ")
connection.close()
