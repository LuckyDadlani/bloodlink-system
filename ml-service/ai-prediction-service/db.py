import pandas as pd
from dotenv import load_dotenv
import os
from sqlalchemy import create_engine

load_dotenv()


def get_engine():
    host     = os.getenv("SUPABASE_HOST")
    port     = os.getenv("SUPABASE_PORT")
    db       = os.getenv("SUPABASE_DB")
    user     = os.getenv("SUPABASE_USER")
    password = os.getenv("SUPABASE_PASSWORD")

    return create_engine(
        f"postgresql+psycopg2://{user}:{password}@{host}:{port}/{db}",
        connect_args={"sslmode": "require"}
    )


def fetch_training_data():
    query = """
        SELECT
            dn.donor_id,
            dn.emergency_id,
            dp.availability_status,
            dp.total_donations,
            dp.last_donation_date,
            dp.latitude         AS donor_lat,
            dp.longitude        AS donor_lng,
            er.hospital_latitude,
            er.hospital_longitude,
            er.urgency_level,
            COALESCE(vw.response_rate, 0.5)            AS response_rate,
            COALESCE(vw.avg_response_time_minutes, 30) AS avg_response_time_minutes,
            EXTRACT(HOUR FROM dn.sent_at)              AS hour_of_day,
            EXTRACT(DOW  FROM dn.sent_at)              AS day_of_week,
            CASE WHEN dn.response_received = 'YES' THEN 1 ELSE 0 END AS responded
        FROM donor_notifications dn
        JOIN donor_profiles dp ON dn.donor_id = dp.donor_id
        JOIN emergency_requests er ON dn.emergency_id = er.emergency_id
        LEFT JOIN vw_donor_response_stats vw ON dn.donor_id = vw.donor_id
        WHERE dn.response_received IS NOT NULL
    """
    df = pd.read_sql(query, get_engine())
    return df


def fetch_eligible_donors(emergency_id):
    query = """
        SELECT
            dp.donor_id,
            1 AS availability_status,
            dp.total_donations,
            dp.last_donation_date,
            dp.latitude          AS donor_lat,
            dp.longitude         AS donor_lng,
            er.hospital_latitude,
            er.hospital_longitude,
            er.urgency_level,
            COALESCE(vw.response_rate, 0.5)            AS response_rate,
            COALESCE(vw.avg_response_time_minutes, 30) AS avg_response_time_minutes,
            EXTRACT(HOUR FROM NOW())                   AS hour_of_day,
            EXTRACT(DOW  FROM NOW())                   AS day_of_week
        FROM vw_eligible_donors dp
        JOIN emergency_requests er ON er.emergency_id = %(emergency_id)s
        LEFT JOIN vw_donor_response_stats vw ON dp.donor_id = vw.donor_id
        WHERE er.blood_group_required::text = dp.blood_group::text
    """

    df = pd.read_sql(
        query,
        get_engine(),
        params={"emergency_id": emergency_id}
    )

    return df