const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

const request = async (path, options = {}) => {
  const headers = {
    ...(options.headers ?? {})
  };
  const hasJsonBody = options.body != null && !(options.body instanceof FormData);
  if (hasJsonBody && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${API_BASE}${path}`, {
    headers,
    ...options
  });

  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch (error) {
      payload = text;
    }
  }

  if (!response.ok) {
    const message = payload?.message ?? payload?.detail ?? payload ?? '请求失败';
    throw new Error(message);
  }
  return payload;
};

export const api = {
  listStudents() {
    return request('/api/students');
  },
  createStudent(body) {
    return request('/api/students', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  updateStudent(studentId, body) {
    return request(`/api/students/${studentId}`, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  },
  getTuitionOverview() {
    return request('/api/students/tuition-overview');
  },
  getWriteOffOverview(month) {
    const suffix = month ? `?month=${encodeURIComponent(month)}` : '';
    return request(`/api/students/write-off-overview${suffix}`);
  },
  listReferrals() {
    return request('/api/referrals');
  },
  createReferral(body) {
    return request('/api/referrals', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  listPaymentRecords() {
    return request('/api/students/payment-records');
  },
  createPayment(studentId, body) {
    return request(`/api/students/${studentId}/payments`, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  listWeekSchedules(startDate) {
    return request(`/api/schedules/week?start=${startDate}`);
  },
  listStudentPlannedSchedules(studentId) {
    return request(`/api/schedules/students/${studentId}/planned`);
  },
  generateSchedules(studentId, body) {
    return request(`/api/schedules/students/${studentId}/auto-generate`, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  createTemporaryLesson(studentId, body) {
    return request(`/api/schedules/students/${studentId}/temporary-lesson`, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  requestScheduleLeave(studentId) {
    return request(`/api/schedules/students/${studentId}/leave`, {
      method: 'POST'
    });
  },
  rescheduleSchedule(scheduleId, body) {
    return request(`/api/schedules/${scheduleId}/reschedule`, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  rescheduleFollowingSchedules(scheduleId, body) {
    return request(`/api/schedules/${scheduleId}/reschedule-following`, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  completeSchedule(scheduleId) {
    return request(`/api/schedules/${scheduleId}/complete`, {
      method: 'POST'
    });
  },
  undoCompleteSchedule(scheduleId) {
    return request(`/api/schedules/${scheduleId}/undo-complete`, {
      method: 'POST'
    });
  },
  listTrials() {
    return request('/api/trials');
  },
  createTrial(body) {
    return request('/api/trials', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },
  updateTrial(trialId, body) {
    return request(`/api/trials/${trialId}`, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  }
};
