export interface HiredCandidate {
    userId: string;
    candidateName: string;
    applicationId: string;
}

export interface InternshipDemand {
    jobId: string;
    jobTitle: string;
    companyName: string;
    preferredBatch: string;
    durationWeeks: number;
    hiredCandidates: HiredCandidate[];
}

export interface CompanyIntern {
    id: string;
    candidateName: string;
    batchName: string;
    mentorEmail: string;
    acceptedTasksCount: number;
    currentScore: number;
}