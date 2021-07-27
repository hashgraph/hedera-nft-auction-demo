FROM postgres:12.6-alpine
# Adjust PostgreSQL configuration so that remote connections to the
# database are possible.
RUN echo "host all  all    0.0.0.0/0  md5" >> /var/lib/postgresql/data/pg_hba.conf

RUN echo "listen_addresses='*'" >> /var/lib/postgresql/data/postgresql.conf
ENV POSTGRES_DB nftauction
COPY initdb.sh /docker-entrypoint-initdb.d/initdb.sh
EXPOSE 5432
