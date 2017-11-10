FROM postgres

ENV POSTGRES_DB crashrec
ENV POSTGRES_USER crashrec
ENV POSTGRES_PASSWORD crashrec

COPY psql_init.sql /docker-entrypoint-initdb.d/

