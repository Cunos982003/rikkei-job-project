import apiClient from './client';

export interface ReportParams {
    from: string;
    to: string;
    companyId?: string;
    jobId?: string;
    courseName?: string;
}

export async function getReport(params: ReportParams) {
    const response = await apiClient.get('/reports/recruitment', { params });
    // Tùy thuộc vào client.ts của bạn trả về gì, có thể là response hoặc response.data
    // Nếu bị lỗi tương tự ở dòng này, hãy đổi thành return response;
    return (response as any).data || response;
}

export async function exportReport(params: ReportParams) {
    const response = await apiClient.get('/reports/recruitment/export', {
        params,
        responseType: 'blob',
    });

    // SỬA LỖI Ở ĐÂY: Ép kiểu response.data thành any để TS không báo lỗi
    const blobData = (response as any).data || response;
    const url = window.URL.createObjectURL(new Blob([blobData as BlobPart]));

    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'recruitment_report.xlsx');
    document.body.appendChild(link);
    link.click();
    link.remove();
}