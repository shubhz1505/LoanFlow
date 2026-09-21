FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN python train.py

EXPOSE 9000

RUN useradd -m loanflow
USER loanflow

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9000", \
     "--workers", "2", "--log-level", "info"]