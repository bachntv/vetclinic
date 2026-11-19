INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Khám tổng quát','CONSULTATION',100000,'Bác sĩ khai thác tiền sử, thăm khám lâm sàng và đánh giá tình trạng sức khỏe. Chủ nuôi được giải thích kết quả khám, tư vấn hướng chăm sóc và trao đổi chi phí dự kiến trước khi thực hiện các dịch vụ bổ sung.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Khám tổng quát');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tái khám','CONSULTATION',50000,'Bác sĩ đánh giá diễn tiến sau lần khám trước, kiểm tra đáp ứng điều trị và cập nhật kế hoạch theo dõi. Chủ nuôi được giải đáp thắc mắc và nhận hướng dẫn chăm sóc tiếp theo; thuốc và xét nghiệm bổ sung được báo phí riêng.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tái khám');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Khám chuyên sâu','CONSULTATION',200000,'Bác sĩ thăm khám tập trung vào vấn đề sức khỏe cần đánh giá thêm, rà soát bệnh sử và các kết quả đã có. Chủ nuôi được giải thích nhận định chuyên môn, phương án kiểm tra tiếp theo và chi phí dự kiến để trao đổi trước khi thực hiện.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Khám chuyên sâu');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Vệ sinh tai','OTHER',60000,'Nhân viên chuyên môn vệ sinh tai theo đánh giá và hướng dẫn của bác sĩ, ghi nhận tình trạng trước và sau chăm sóc. Chủ nuôi được hướng dẫn theo dõi tại nhà; thuốc điều trị hoặc kiểm tra bổ sung được tư vấn và báo phí riêng.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Vệ sinh tai');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Thay băng, vệ sinh vết thương','OTHER',80000,'Bác sĩ hoặc nhân viên chuyên môn đánh giá vết thương, vệ sinh và thay băng phù hợp. Chủ nuôi được thông báo diễn tiến, hướng dẫn chăm sóc và lịch kiểm tra tiếp theo. Thuốc và can thiệp bổ sung được trao đổi chi phí trước khi thực hiện.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Thay băng, vệ sinh vết thương');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Lưu viện (mỗi ngày)','HOSPITAL',150000,'Bố trí khu lưu viện, chăm sóc cơ bản và theo dõi theo kế hoạch của bác sĩ trong một ngày lưu viện. Tình trạng và các chăm sóc thực hiện được ghi vào hồ sơ. Chủ nuôi được cập nhật diễn tiến, trao đổi kế hoạch điều trị và nhận hướng dẫn khi xuất viện; thuốc, xét nghiệm và thủ thuật phát sinh được báo phí riêng trước khi thực hiện.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Lưu viện (mỗi ngày)');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Siêu âm','OTHER',200000,'Bác sĩ thực hiện siêu âm vùng cần khảo sát theo chỉ định, ghi nhận kết quả và giải thích nhận định trong bối cảnh thăm khám. Chủ nuôi được trao đổi về bước kiểm tra hoặc theo dõi tiếp theo. Các dịch vụ bổ sung được báo phí riêng.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Siêu âm');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Nobivac 3-Rabies','VACCINATION',120000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Nobivac 3-Rabies');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Nobivac Canine 1-DAPPv','VACCINATION',250000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Nobivac Canine 1-DAPPv');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Nobivac Feline 3-HCP','VACCINATION',300000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Nobivac Feline 3-HCP');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Vanguard DAPP','VACCINATION',250000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Vanguard DAPP');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Vanguard Feline RCP','VACCINATION',300000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Vanguard Feline RCP');

INSERT INTO clinic_services (name,category,unit_price,description,active,created_at,updated_at)
SELECT 'Tiêm Vanguard Rabies 3 Year','VACCINATION',120000,'Bác sĩ đánh giá tình trạng sức khỏe và thực hiện tiêm chủng với vaccine phù hợp. Hồ sơ ghi nhận tên vaccine, số lô, ngày tiêm và lịch hẹn tiếp theo nếu có. Chủ nuôi được hướng dẫn theo dõi sau tiêm và cách liên hệ phòng khám khi cần hỗ trợ.',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM clinic_services WHERE name='Tiêm Vanguard Rabies 3 Year');
